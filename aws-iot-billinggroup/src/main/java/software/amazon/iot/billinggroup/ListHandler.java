package software.amazon.iot.billinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListBillingGroupsRequest;
import software.amazon.awssdk.services.iot.model.ListBillingGroupsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ListHandler:
 * ListBillingGroups: To retrieve a list of all BillingGroups in the account
 */
public class ListHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();

        final ListBillingGroupsRequest listBillingGroupsRequest = Translator.translateToListRequest(request.getNextToken());
        try {
            ListBillingGroupsResponse listBillingGroupsResponse =
                    proxy.injectCredentialsAndInvokeV2(
                            listBillingGroupsRequest,
                            proxyClient.client()::listBillingGroups
                    );

            String nextToken = listBillingGroupsResponse.nextToken();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListRequest(listBillingGroupsResponse))
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }
    }
}
