package software.amazon.iot.billinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Set;

/**
 * API Calls for ReadHandler:
 * DescribeBillingGroup: To retrieve all properties of a new/updated BillingGroup
 * ListTagsForResource: To retrieve all tags associated with BillingGroup
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
        final DescribeBillingGroupRequest describeBillingGroupRequest = Translator.translateToReadRequest(resourceModel);

        try {
            DescribeBillingGroupResponse describeBillingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeBillingGroupRequest,
                    proxyClient.client()::describeBillingGroup
            );
            logger.log(String.format("%s %s has successfully been read.",
                    ResourceModel.TYPE_NAME,describeBillingGroupRequest.billingGroupName()));

            final String arn = describeBillingGroupResponse.billingGroupArn();
            final String id = describeBillingGroupResponse.billingGroupId();
            final String billingGroupName = describeBillingGroupResponse.billingGroupName();
            String billingGroupDescription = "";
            if (describeBillingGroupResponse.billingGroupProperties() != null) {
                billingGroupDescription = describeBillingGroupResponse.billingGroupProperties()
                        .billingGroupDescription();
            }
            resourceModel.setArn(arn);
            resourceModel.setId(id);
            resourceModel.setBillingGroupName(billingGroupName);
            resourceModel.setBillingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                    .billingGroupDescription(billingGroupDescription)
                    .build());

            final ListTagsForResourceResponse listResourceTagsResponse = proxyClient.injectCredentialsAndInvokeV2(
                    Translator.listResourceTagsRequest(resourceModel),
                    proxyClient.client()::listTagsForResource
            );
            final Set<Tag> tags = Translator.translateTagsFromSdk(listResourceTagsResponse.tags());
            logger.log(String.format("Listed Tags for %s %s.",
                    ResourceModel.TYPE_NAME,describeBillingGroupRequest.billingGroupName()));
            resourceModel.setTags(tags);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
