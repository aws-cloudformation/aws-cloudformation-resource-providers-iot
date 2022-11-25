package software.amazon.iot.billinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
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

    private static final String OPERATION = "ListBillingGroups";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        try {
            final ListBillingGroupsRequest listBillingGroupsRequest = Translator.translateToListRequest(request.getNextToken());
            ListBillingGroupsResponse listBillingGroupsResponse =
                    proxy.injectCredentialsAndInvokeV2(
                            listBillingGroupsRequest,
                            proxyClient.client()::listBillingGroups
                    );
            String nextToken = listBillingGroupsResponse.nextToken();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(listBillingGroupsResponse))
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(null, OPERATION, e);
        }
    }
}
