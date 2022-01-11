package software.amazon.iot.billinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for DeleteHandler:
 * DeleteBillingGroup: To delete a BillingGroup
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
        final DeleteBillingGroupRequest deleteBillingGroupRequest = Translator.translateToDeleteRequest(resourceModel);
        final DescribeBillingGroupRequest describeBillingGroupRequest = Translator.translateToReadRequest(resourceModel);

        try {
            // check whether the resource exists - ResourceNotFound is thrown otherwise.
            proxyClient.injectCredentialsAndInvokeV2(
                    describeBillingGroupRequest,
                    proxyClient.client()::describeBillingGroup
            );
            logger.log(String.format("%s %s Exists. Proceed to delete the resource.",
                    ResourceModel.TYPE_NAME, describeBillingGroupRequest.billingGroupName()));

            // perform delete operation
            proxyClient.injectCredentialsAndInvokeV2(
                    deleteBillingGroupRequest,
                    proxyClient.client()::deleteBillingGroup
            );
            logger.log(String.format("%s %s successfully deleted.",
                    ResourceModel.TYPE_NAME, deleteBillingGroupRequest.billingGroupName()));
        } catch (final Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
