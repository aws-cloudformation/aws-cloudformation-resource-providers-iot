package software.amazon.iot.thing;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ReadHandler:
 * DescribeThing: To retrieve all properties of a new/updated Thing
 */
public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final DescribeThingRequest describeThingRequest = Translator.translateToReadRequest(resourceModel);

        try {
            DescribeThingResponse describeThingResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingRequest,
                    proxyClient.client()::describeThing
            );
            logger.log(String.format("%s %s has successfully been read.",
                    ResourceModel.TYPE_NAME, describeThingResponse.thingName()));

            resourceModel.setArn(describeThingResponse.thingArn());
            resourceModel.setId(describeThingResponse.thingId());
            resourceModel.setThingName(describeThingResponse.thingName());
            resourceModel.setAttributePayload(Translator.translateToModelAttributePayload(describeThingResponse.attributes()));
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
