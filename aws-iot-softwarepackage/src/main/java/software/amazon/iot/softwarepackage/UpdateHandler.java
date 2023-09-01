package software.amazon.iot.softwarepackage;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdatePackageRequest;
import software.amazon.awssdk.services.iot.model.UpdatePackageResponse;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.iot.cfn.common.handler.Tagging;


/**
 * API Calls for UpdateHandler:
 * UpdateSoftwarePackage: To update the properties of SoftwarePackage
 * GetSoftwarePackage: To retrieve ARN of the SoftwarePackage to make Tag and UnTag API calls
 * ListTagsForResource: To retrieve old tags associated with SoftwarePackage
 */
public class UpdateHandler extends BaseHandlerStd {

    private static final String OPERATION = "UpdateSoftwarePackage";
    private static final String CALL_GRAPH = "AWS-IoT-SoftwarePackage::Update";
    private static final String CALL_GRAPH_TAG = "AWS-IoT-SoftwarePackage::TaggingUpdate";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel prevModel = request.getPreviousResourceState() == null ?
                request.getDesiredResourceState() : request.getPreviousResourceState();
        ResourceModel newModel = request.getDesiredResourceState();

        validatePropertiesAreUpdatable(newModel, prevModel);

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .makeServiceCall(this::updateResource)
                                .progress())
                .then(progress ->
                        proxy.initiate(CALL_GRAPH_TAG, proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToReadRequest)
                                .makeServiceCall((getRequest, proxyInvocation) -> {
                                    try {
                                        return proxyInvocation.injectCredentialsAndInvokeV2(getRequest,
                                                proxyInvocation.client()::getPackage);
                                    } catch (IotException e) {
                                        throw Translator.translateIotExceptionToHandlerException(getRequest.packageName(), OPERATION, e);
                                    }
                                })
                                .stabilize(this::stabilizeUpdateTags)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request.toBuilder().desiredResourceState(newModel).build(), callbackContext, proxyClient, logger));
    }

    private void validatePropertiesAreUpdatable(ResourceModel newModel, ResourceModel prevModel) {
        if (!StringUtils.equals(newModel.getPackageName(), prevModel.getPackageName())) {
            throwCfnNotUpdatableException("SoftwarePackageName");
        } else if (StringUtils.isNotEmpty(newModel.getPackageArn()) && !StringUtils.equals(newModel.getPackageArn(), prevModel.getPackageArn())) {
            throwCfnNotUpdatableException("Arn");
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidRequestException.builder()
                .message(String.format("Parameter '%s' is not updatable.", propertyName))
                .build());
    }

    private Boolean stabilizeUpdateTags(final GetPackageRequest request, final GetPackageResponse response,
                                        ProxyClient<IotClient> proxyClient, ResourceModel model, CallbackContext context) {
        return Tagging.updateResourceTags(response.packageArn(), model.getPackageName(), OPERATION,
                ResourceModel.TYPE_NAME, model.getTags(), proxyClient);
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param updatePackageRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdatePackageResponse updateResource(
            UpdatePackageRequest updatePackageRequest,
            ProxyClient<IotClient> proxyClient) {
        // TODO: add in client token once idempotency is implemented for the API
        UpdatePackageRequest requestWithClientToken = UpdatePackageRequest.builder()
                .packageName(updatePackageRequest.packageName())
                .description(updatePackageRequest.description())
                .defaultVersionName(updatePackageRequest.defaultVersionName())
                .unsetDefaultVersion(updatePackageRequest.unsetDefaultVersion())
                //.clientToken(this.clientToken)
                .build();
        try {
            final UpdatePackageResponse updatePackageResponse = proxyClient.injectCredentialsAndInvokeV2(
                    requestWithClientToken, proxyClient.client()::updatePackage);
            logger.log(String.format("%s [%s] has been successfully updated.",
                    ResourceModel.TYPE_NAME, updatePackageRequest.packageName()));
            return updatePackageResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updatePackageRequest.packageName(), OPERATION, e);
        }
    }
}
