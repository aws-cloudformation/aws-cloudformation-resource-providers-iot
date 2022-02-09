package software.amazon.iot.thinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Set;

/**
 * API Calls for ReadHandler:
 * DescribeThingGroup: To retrieve all properties of a new/updated ThingGroup
 * ListTagsForResource: To retrieve all tags associated with ThingGroup
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
        final DescribeThingGroupRequest describeThingGroupRequest = Translator.translateToReadRequest(resourceModel);

        try {
            DescribeThingGroupResponse describeThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingGroupRequest,
                    proxyClient.client()::describeThingGroup
            );
            logger.log(String.format("%s %s has successfully been read.",
                    ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName()));

            final String arn = describeThingGroupResponse.thingGroupArn();
            final String id = describeThingGroupResponse.thingGroupId();
            final String thingGroupName = describeThingGroupResponse.thingGroupName();
            String parentGroupName = null;
            if (describeThingGroupResponse.thingGroupMetadata() != null &&
                    describeThingGroupResponse.thingGroupMetadata().parentGroupName() != null) {
                parentGroupName = describeThingGroupResponse.thingGroupMetadata().parentGroupName();
            }
            resourceModel.setId(id);
            resourceModel.setArn(arn);
            resourceModel.setThingGroupName(thingGroupName);
            resourceModel.setParentGroupName(parentGroupName);
            resourceModel.setThingGroupProperties(
                    Translator.translateThingGroupPropertiesToModelObject(
                            describeThingGroupResponse.thingGroupProperties())
            );

            final ListTagsForResourceResponse listResourceTagsResponse = proxyClient.injectCredentialsAndInvokeV2(
                    Translator.listResourceTagsRequest(resourceModel),
                    proxyClient.client()::listTagsForResource
            );
            final Set<Tag> tags = Translator.translateTagsFromSdk(listResourceTagsResponse.tags());
            logger.log(String.format("Listed Tags for %s %s.",
                    ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName()));
            resourceModel.setTags(tags);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
