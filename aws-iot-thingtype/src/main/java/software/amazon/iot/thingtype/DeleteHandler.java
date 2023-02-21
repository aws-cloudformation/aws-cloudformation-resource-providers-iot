package software.amazon.iot.thingtype;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

/**
 * The handler deletes the THING-TYPE resource (if it exists)
 *
 * API Calls for DeleteHandler:
 * DeprecateThingType: To deprecate a ThingType
 * DeleteThingType: To delete a ThingType
 */
public class DeleteHandler extends BaseHandlerStd {

    private static final String OPERATION = "DeleteThingType";
    private static final String CALL_GRAPH = "AWS-IoT-ThingType::Delete";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        if (StringUtils.isEmpty(resourceModel.getThingTypeName())) {
            throw new CfnNotFoundException(InvalidRequestException.builder()
                    .message("Parameter 'ThingTypeName' must be provided.")
                    .build());
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest((ResourceModel model) ->
                                        Translator.translateToDeprecateRequest(model, true))
                                .backoffDelay(i -> Duration.ofSeconds(120))
                                .makeServiceCall(this::deprecateResource)
                                .stabilize(this::stabilizeOnDeprecateAndDelete)
                                .done(response -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.SUCCESS)
                                .build()));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deprecateThingTypeRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private DeprecateThingTypeResponse deprecateResource(
            final DeprecateThingTypeRequest deprecateThingTypeRequest,
            final ProxyClient<IotClient> proxyClient) {
        try {
            DeprecateThingTypeResponse deprecateThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                    deprecateThingTypeRequest, proxyClient.client()::deprecateThingType);
            logger.log(String.format("%s %s successfully deprecated.",
                    ResourceModel.TYPE_NAME, deprecateThingTypeRequest.thingTypeName()));
            return deprecateThingTypeResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deprecateThingTypeRequest.thingTypeName(), OPERATION, e);
        }
    }

    private boolean stabilizeOnDeprecateAndDelete(
            final DeprecateThingTypeRequest deprecateThingTypeRequest,
            final DeprecateThingTypeResponse deprecateThingTypeResponse,
            final ProxyClient<IotClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {
        boolean stabilized = true;

        try {
            proxyClient.injectCredentialsAndInvokeV2(
                    Translator.translateToDeleteRequest(model),
                    proxyClient.client()::deleteThingType
            );
            logger.log(String.format("%s [%s] deletion has stabilized: %s",
                    ResourceModel.TYPE_NAME, model.getThingTypeName(), true));
        } catch (IotException e) {
            if (e.getMessage() != null && e.getMessage().contains("minutes after deprecation and then retry")) {
                stabilized = false;
            } else {
                throw Translator.translateIotExceptionToHandlerException(deprecateThingTypeRequest.thingTypeName(), OPERATION, e);
            }
        }
        return stabilized;
    }
}
