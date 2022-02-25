package software.amazon.iot.thinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for DeleteHandler:
 * DeleteThingGroup: To delete a ThingGroup
 * DeleteDynamicThingGroup: To delete a Dynamic ThingGroup
 * DescribeThingGroup: used for the following purpose -
 *  - To verify whether the thing group exists
 *  - To check whether the ThingGroup to be deleted has a queryString; Dynamic Thing group would have the param set
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
        final DescribeThingGroupRequest describeThingGroupRequest = Translator.translateToReadRequest(resourceModel);

        try {
            // check whether the resource exists - ResourceNotFound is thrown otherwise.
            DescribeThingGroupResponse describeThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingGroupRequest,
                    proxyClient.client()::describeThingGroup
            );
            logger.log(String.format("%s %s Exists. Proceed to delete the resource.",
                    ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName()));

            // perform delete operation
            if (isDynamicThingGroup(describeThingGroupResponse)) {
                final DeleteDynamicThingGroupRequest deleteDynamicThingGroupRequest =
                        Translator.translateToDeleteDynamicThingGroupRequest(resourceModel);

                proxyClient.injectCredentialsAndInvokeV2(
                        deleteDynamicThingGroupRequest,
                        proxyClient.client()::deleteDynamicThingGroup
                );
                logger.log(String.format("%s %s (DynamicThingGroup) successfully deleted.",
                        ResourceModel.TYPE_NAME, deleteDynamicThingGroupRequest.thingGroupName()));
            } else {
                final DeleteThingGroupRequest deleteThingGroupRequest =
                        Translator.translateToDeleteThingGroupRequest(resourceModel);

                proxyClient.injectCredentialsAndInvokeV2(
                        deleteThingGroupRequest,
                        proxyClient.client()::deleteThingGroup
                );
                logger.log(String.format("%s %s successfully deleted.",
                        ResourceModel.TYPE_NAME, deleteThingGroupRequest.thingGroupName()));
            }
        } catch (final Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
