package software.amazon.iot.softwarepackage;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPackagesRequest;
import software.amazon.awssdk.services.iot.model.ListPackagesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ListHandler:
 * ListPackages: To list all Packages in an account
 */
public class ListHandler extends BaseHandlerStd {

    private static final String OPERATION = "ListSoftwarePackages";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {
        final String awsAccountId = request.getAwsAccountId();

        logger.log(String.format("%s for accountId: %s",
                OPERATION, awsAccountId));

        try {
            final ListPackagesRequest listPackagesRequest = Translator.translateToListRequest(request.getNextToken());
            ListPackagesResponse listPackagesResponse = proxy.injectCredentialsAndInvokeV2(
                    listPackagesRequest,
                    proxyClient.client()::listPackages
            );
            String nextToken = listPackagesResponse.nextToken();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(listPackagesResponse))
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(null, OPERATION, e);
        }
    }
}
