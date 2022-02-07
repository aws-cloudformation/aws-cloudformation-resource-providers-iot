package software.amazon.iot.billinggroup;

import com.google.common.collect.Sets;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.UpdateBillingGroupRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * API Calls for UpdateHandler:
 * UpdateBillingGroup: To update the properties of BillingGroup
 * DescribeBillingGroup: To retrieve ARN of the BillingGroup to make Tag and UnTag API calls
 * ListTagsForResource: To retrieve old tags associated with BillingGroup
 * UntagResource: To remove old tags
 * TagResource: To add new tags
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
        final Set<Tag> desiredTags = Translator.translateTagsToSdk(request.getDesiredResourceTags());
        final DescribeBillingGroupRequest describeBillingGroupRequest = Translator.translateToReadRequest(resourceModel);
        final UpdateBillingGroupRequest updateBillingGroupRequest = Translator.translateToUpdateRequest(resourceModel);

        try {
            // check whether the resource exists - ResourceNotFound is thrown otherwise.
            DescribeBillingGroupResponse describeBillingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeBillingGroupRequest,
                    proxyClient.client()::describeBillingGroup
            );
            logger.log(String.format("%s %s Exists. Proceed to update.",
                    ResourceModel.TYPE_NAME,describeBillingGroupRequest.billingGroupName()));
            resourceModel.setArn(describeBillingGroupResponse.billingGroupArn());

            // update changes
            proxyClient.injectCredentialsAndInvokeV2(
                    updateBillingGroupRequest,
                    proxyClient.client()::updateBillingGroup
            );
            logger.log(String.format("%s %s has successfully been updated.",
                    ResourceModel.TYPE_NAME, updateBillingGroupRequest.billingGroupName()));

            // update the Tags as specified in the resource model by generating a diff of current and desired Tags
            updateTags(proxyClient, resourceModel, desiredTags);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }

    private void updateTags(ProxyClient<IotClient> proxyClient, ResourceModel resourceModel, Set<Tag> desiredTags) {
        final ListTagsForResourceRequest listTagsForResourceRequest = Translator.listResourceTagsRequest(resourceModel);
        ListTagsForResourceResponse listTagsForResourceResponse = proxyClient.injectCredentialsAndInvokeV2(
                listTagsForResourceRequest,
                proxyClient.client()::listTagsForResource
        );
        logger.log(String.format("Listed Tags for %s %s",
                ResourceModel.TYPE_NAME, listTagsForResourceRequest.resourceArn()));
        final Set<Tag> existingTags = new HashSet<>(listTagsForResourceResponse.tags());
        final Set<Tag> tagsToRemove = Sets.difference(existingTags, desiredTags);
        final Set<Tag> tagsToAdd = Sets.difference(desiredTags, existingTags);
        // API call to remove old tags
        if (!tagsToRemove.isEmpty()) {
            proxyClient.injectCredentialsAndInvokeV2(
                    Translator.untagResourceRequest(resourceModel.getArn(), tagsToRemove),
                    proxyClient.client()::untagResource
            );
            logger.log(String.format("Removed old Tags for %s %s",
                    ResourceModel.TYPE_NAME, listTagsForResourceRequest.resourceArn()));
        }
        // API call to add new tags
        if (!tagsToAdd.isEmpty()) {
            proxyClient.injectCredentialsAndInvokeV2(
                    Translator.tagResourceRequest(resourceModel.getArn(), tagsToAdd),
                    proxyClient.client()::tagResource
            );
            logger.log(String.format("Added new Tags for %s %s",
                    ResourceModel.TYPE_NAME, listTagsForResourceRequest.resourceArn()));
        }
    }
}
