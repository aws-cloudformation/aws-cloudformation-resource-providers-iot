package software.amazon.iot.thing;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.UpdateThingRequest;
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
        final UpdateThingRequest updateThingRequest = Translator.translateToUpdateRequest(resourceModel);

        try {
            // check whether the resource exists - ResourceNotFound is thrown otherwise.
            DescribeThingResponse describeThingResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingRequest,
                    proxyClient.client()::describeThing
            );
            logger.log(String.format("%s %s Exists. Proceed to update.",
                    ResourceModel.TYPE_NAME, describeThingRequest.thingName()));
            resourceModel.setArn(describeThingResponse.thingArn());

            // update changes
            proxyClient.injectCredentialsAndInvokeV2(
                    updateThingRequest,
                    proxyClient.client()::updateThing
            );
            logger.log(String.format("%s %s has successfully been updated.",
                    ResourceModel.TYPE_NAME, updateThingRequest.thingName()));
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
