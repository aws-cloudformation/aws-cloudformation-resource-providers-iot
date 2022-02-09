package software.amazon.iot.thingtype;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

/**
 * The handler deletes the THING-TYPE resource (if it exists)
 *
 * API Calls for DeleteHandler:
 * DescribeThingType: To check whether the resource exists; throw "NotFound" status code otherwise
 * DeprecateThingType: To deprecate a ThingType
 * DeleteThingType: To delete a ThingType
 */
public class DeleteHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final DescribeThingTypeRequest describeThingTypeRequest = Translator.translateToReadRequest(resourceModel);

        try {
            // check whether the resource exists - ResourceNotFound is thrown otherwise.
            proxyClient.injectCredentialsAndInvokeV2(
                    describeThingTypeRequest,
                    proxyClient.client()::describeThingType
            );
            logger.log(String.format("%s %s Exists. Proceed to delete the resource.",
                    ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName()));

            // perform delete operation
            return ProgressEvent.progress(resourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate("AWS-IoT-ThingType::Delete", proxyClient, resourceModel, callbackContext)
                                    .translateToServiceRequest((ResourceModel model) ->
                                            Translator.translateToDeprecateRequest(model, true))
                                    .backoffDelay(i -> Duration.ofSeconds(120))
                                    .makeServiceCall(this::deprecateResource)
                                    .stabilize(this::stabilizedOnDelete)
                                    .handleError(this::handleError)
                                    .done(response -> ProgressEvent.defaultSuccessHandler(null)));
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }
    }

    private DeprecateThingTypeResponse deprecateResource(
            final DeprecateThingTypeRequest deprecateThingTypeRequest,
            final ProxyClient<IotClient> proxyClient) {
        DeprecateThingTypeResponse deprecateThingTypeResponse;
        deprecateThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                deprecateThingTypeRequest,
                proxyClient.client()::deprecateThingType
        );
        logger.log(String.format("%s %s successfully deprecated.",
                ResourceModel.TYPE_NAME, deprecateThingTypeRequest.thingTypeName()));
        return deprecateThingTypeResponse;
    }

    private boolean stabilizedOnDelete(
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
                    ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), true));
        } catch(InvalidRequestException e) {
            if (e.getMessage().contains("minutes after deprecation and then retry")) {
                stabilized = false;
            } else {
                throw e;
            }
        } catch (ResourceNotFoundException e) {
            // let default success handler to return
            logger.log(String.format("%s %s is already deleted",
                    ResourceModel.TYPE_NAME, model.getThingTypeName()));
        }
        return stabilized;
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleError(
            DeprecateThingTypeRequest deprecateThingTypeRequest,
            Exception e,
            ProxyClient<IotClient> iotClientProxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext
    ) {
        return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
    }
}
