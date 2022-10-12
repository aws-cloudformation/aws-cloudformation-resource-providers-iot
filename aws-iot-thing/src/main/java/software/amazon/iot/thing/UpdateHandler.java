package software.amazon.iot.thing;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdateThingRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * The handler updates the THING resource (if it exists)
 * API Calls for UpdateHandler:
 * DescribeThing: To check whether the resource exists; throw "NotFound" status code otherwise
 * UpdateThing: To update a Thing
 */
public class UpdateHandler extends BaseHandlerStd {

    private static final String OPERATION = "UpdateThing";
    private static final String CALL_GRAPH = "AWS-IoT-Thing::Update";
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
        newModel.setThingName(prevModel.getThingName());

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .makeServiceCall(this::updateResource)
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(newModel));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param updateThingRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdateThingResponse updateResource(
            UpdateThingRequest updateThingRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            final UpdateThingResponse updateThingResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updateThingRequest, proxyClient.client()::updateThing);
            logger.log(String.format("%s [%s] has been successfully updated.",
                    ResourceModel.TYPE_NAME, updateThingRequest.thingName()));
            return updateThingResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updateThingRequest.thingName(), OPERATION, e);
        }
    }
}
