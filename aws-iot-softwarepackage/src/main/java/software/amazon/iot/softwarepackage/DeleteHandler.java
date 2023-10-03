package software.amazon.iot.softwarepackage;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeletePackageRequest;
import software.amazon.awssdk.services.iot.model.DeletePackageResponse;
import software.amazon.awssdk.services.iot.model.DeletePackageVersionRequest;
import software.amazon.awssdk.services.iot.model.DeletePackageVersionResponse;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPackageVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPackageVersionsResponse;
import software.amazon.awssdk.services.iot.model.PackageVersionSummary;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UpdatePackageRequest;
import software.amazon.awssdk.services.iot.model.UpdatePackageResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

/**
 * The handler deletes the Package resource (if it exists)
 * API Calls for DeleteHandler:
 * DeletePackage: To delete a Package if exists, return not found status otherwise
 */
public class DeleteHandler extends BaseHandlerStd {

    private static final String OPERATION = "DeleteSoftwarePackage";
    private static final String CALL_GRAPH = "AWS-IoT-SoftwarePackage::Delete";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();

        // eagerly return not found if name is not provided in the request
        if (StringUtils.isEmpty(resourceModel.getPackageName())) {
            throw new CfnNotFoundException(InvalidRequestException.builder()
                    .message("Parameter 'PackageName' must be provided.")
                    .build());
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToListRequestForPackageVersion)
                                .makeServiceCall(this::listThenDeleteResourceForPackageVersion)
                                .stabilize(this::stabilizedOnDeleteForPackageVersion)
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
     * @param deletePackageRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeletePackageResponse deleteResource(
            DeletePackageRequest deletePackageRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            checkForPackage(deletePackageRequest.packageName(), proxyClient);
            DeletePackageResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    deletePackageRequest, proxyClient.client()::deletePackage);
            logger.log(String.format("%s [%s] successfully deleted.",
                    ResourceModel.TYPE_NAME, deletePackageRequest.packageName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deletePackageRequest.packageName(), OPERATION, e);
        }
    }

    private ListPackageVersionsResponse listThenDeleteResourceForPackageVersion(
            ListPackageVersionsRequest listPackageVersionsRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            String packageName = listPackageVersionsRequest.packageName();
            ListPackageVersionsResponse listPackageVersionsResponse;
            do {
                listPackageVersionsResponse = proxyClient.injectCredentialsAndInvokeV2(
                        listPackageVersionsRequest, proxyClient.client()::listPackageVersions);
                if (listPackageVersionsResponse.hasPackageVersionSummaries()) {
                    List<PackageVersionSummary> packageVersionSummaries = listPackageVersionsResponse.packageVersionSummaries();

                    packageVersionSummaries.stream().forEach(packageVersionSummary -> {
                        DeletePackageVersionRequest deletePackageVersionRequest = DeletePackageVersionRequest.builder()
                                .packageName(packageName)
                                .versionName(packageVersionSummary.versionName())
                                .build();
                        proxyClient.injectCredentialsAndInvokeV2(
                                deletePackageVersionRequest, proxyClient.client()::deletePackageVersion);
                        logger.log(String.format("%s [%s, %s] successfully deleted.",
                                ResourceModel.TYPE_NAME, deletePackageVersionRequest.packageName(), deletePackageVersionRequest.versionName()));
                    });
                }
                listPackageVersionsRequest = ListPackageVersionsRequest.builder()
                        .packageName(packageName)
                        .nextToken(listPackageVersionsResponse.nextToken())
                        .build();
            } while (listPackageVersionsResponse.nextToken() != null);
            return listPackageVersionsResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(listPackageVersionsRequest.packageName(), OPERATION, e);
        }
    }

    private UpdatePackageResponse updateResourceToUnsetDefaultVersion(
            UpdatePackageRequest updatePackageRequest,
            ProxyClient<IotClient> proxyClient) {
        // cannot pass full request because of defaultversion and unsetdefaultversion cannot be in the same request
        UpdatePackageRequest requestTrimmed = UpdatePackageRequest.builder()
                .packageName(updatePackageRequest.packageName())
                .unsetDefaultVersion(updatePackageRequest.unsetDefaultVersion())
                .build();
        try {
            UpdatePackageResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    requestTrimmed, proxyClient.client()::updatePackage);
            logger.log(String.format("%s [%s] successfully updated.",
                    ResourceModel.TYPE_NAME, updatePackageRequest.packageName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updatePackageRequest.packageName(), OPERATION, e);
        }
    }

    private void checkForPackage(String packageName, ProxyClient<IotClient> proxyClient) {
        try {
            final GetPackageRequest getPackageRequest = GetPackageRequest.builder()
                    .packageName(packageName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(getPackageRequest, proxyClient.client()::getPackage);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(packageName, OPERATION, e);
            }
        }
    }

    private void checkForPackageVersions(String packageName, ProxyClient<IotClient> proxyClient) {
        try {
            final ListPackageVersionsRequest listPackageVersionsRequest = ListPackageVersionsRequest.builder()
                    .packageName(packageName)
                    .build();
            ListPackageVersionsResponse listPackagesVersionResponse = proxyClient.injectCredentialsAndInvokeV2(listPackageVersionsRequest, proxyClient.client()::listPackageVersions);
            if (!listPackagesVersionResponse.hasPackageVersionSummaries() || listPackagesVersionResponse.packageVersionSummaries().size() == 0) {
                throw ResourceNotFoundException.builder().build();
            }
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(packageName, OPERATION, e);
            }
        }
    }

    private Boolean stabilizedOnDelete(
            DeletePackageRequest deletePackageRequest,
            DeletePackageResponse deletePackageResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        try {
            checkForPackage(deletePackageRequest.packageName(), proxyClient);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }

    private Boolean stabilizedOnDeleteForPackageVersion(
            ListPackageVersionsRequest listPackageVersionRequest,
            ListPackageVersionsResponse listPackageVersionResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        //return true;
        try {
            checkForPackageVersions(listPackageVersionRequest.packageName(), proxyClient);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }
}
