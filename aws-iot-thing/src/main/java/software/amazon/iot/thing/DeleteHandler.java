package software.amazon.iot.thing;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * The handler deletes the THING resource (if it exists)
 * API Calls for DeleteHandler:
 * DescribeThing: To check whether the resource exists; throw "NotFound" status code otherwise
 * DeleteThing: To delete a Thing
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
        final DescribeThingRequest describeThingRequest = Translator.translateToReadRequest(resourceModel);
        final DeleteThingRequest deleteThingRequest = Translator.translateToDeleteRequest(resourceModel);

        try {
            // check whether the resource exists - ResourceNotFound is thrown otherwise.
            proxyClient.injectCredentialsAndInvokeV2(
                    describeThingRequest,
                    proxyClient.client()::describeThing
            );
            logger.log(String.format("%s %s Exists. Proceed to delete the resource.",
                    ResourceModel.TYPE_NAME, describeThingRequest.thingName()));

            // perform delete operation
            proxyClient.injectCredentialsAndInvokeV2(
                    deleteThingRequest,
                    proxyClient.client()::deleteThing
            );
            logger.log(String.format("%s %s successfully deleted.",
                    ResourceModel.TYPE_NAME, deleteThingRequest.thingName()));
        } catch (final Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
