package software.amazon.iot.softwarepackageversion;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeletePackageVersionRequest;
import software.amazon.awssdk.services.iot.model.DeletePackageVersionResponse;
import software.amazon.awssdk.services.iot.model.GetPackageVersionRequest;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * The handler deletes the PackageVersion resource (if it exists)
 * API Calls for DeleteHandler:
 * DeletePackageVersion: To delete a PackageVersion if exists, return not found status otherwise
 */
public class DeleteHandler extends BaseHandlerStd {

    private static final String OPERATION = "DeleteSoftwarePackageVersion";
    private static final String CALL_GRAPH = "AWS-IoT-SoftwarePackageVersion::Delete";
    private Logger logger;
    private String clientToken;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        this.clientToken = request.getClientRequestToken();

        final ResourceModel resourceModel = request.getDesiredResourceState();

        if (StringUtils.isEmpty(resourceModel.getPackageName()) || StringUtils.isEmpty(resourceModel.getVersionName())) {
            throw new CfnNotFoundException(InvalidRequestException.builder()
                    .message("Parameter 'PackageName' and 'VersionName' must be provided.")
                    .build());
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateFIRequest)
                                .makeServiceCall(this::updateIndexingConfiguration)
                                .progress())
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.SUCCESS)
                                        .build()));
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deletePackageVersionRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeletePackageVersionResponse deleteResource(
            DeletePackageVersionRequest deletePackageVersionRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            checkForPackageVersion(deletePackageVersionRequest.packageName(), deletePackageVersionRequest.versionName(), proxyClient);
            DeletePackageVersionResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    deletePackageVersionRequest, proxyClient.client()::deletePackageVersion);
            logger.log(String.format("%s [%s, %s] successfully deleted.",
                    ResourceModel.TYPE_NAME, deletePackageVersionRequest.packageName(), deletePackageVersionRequest.versionName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deletePackageVersionRequest.packageName() + ":" + deletePackageVersionRequest.versionName(), OPERATION, e);
        }
    }

    private void checkForPackageVersion(String packageName, String versionName, ProxyClient<IotClient> proxyClient) {
        try {
            final GetPackageVersionRequest getPackageVersionRequest = GetPackageVersionRequest.builder()
                    .packageName(packageName)
                    .versionName(versionName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(getPackageVersionRequest, proxyClient.client()::getPackageVersion);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(packageName + ":" + versionName, OPERATION, e);
            }
        }
    }

    private Boolean stabilizedOnDelete(
            DeletePackageVersionRequest deletePackageVersionRequest,
            DeletePackageVersionResponse deletePackageVersionResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        try {
            checkForPackageVersion(deletePackageVersionRequest.packageName(), deletePackageVersionRequest.versionName(), proxyClient);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }
}
