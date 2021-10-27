package software.amazon.iot.logging;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsResponse;
import software.amazon.awssdk.services.iot.model.SetV2LoggingOptionsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Update handler function is invoked when there is a cloudformation stack update operation
 * 1. Call GetV2LoggingOption to check if resource already exists.
 * 2. Call SetV2LogingOption to reconfigure the account-level logging.
 */
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public UpdateHandler() {
        iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        /**
         * Note: Unlike CreateHandler, there's no need to verify the accountID in the model vs the request.
         * Since it's a primaryIdentifier, upon change, CFN will issue Create+Delete rather than just Update.
         * The Create will fail in that case because it does have the account ID check.
         */
        String accountId = request.getAwsAccountId();

        try {
            GetV2LoggingOptionsResponse response = proxy.injectCredentialsAndInvokeV2(GetV2LoggingOptionsRequest.builder().build(), iotClient::getV2LoggingOptions);
            logger.log(String.format("Get %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));

            // Throw notFound when disableAllLogs is true as it indicates resource doesn't exist
            if (response.disableAllLogs()) {
                return ProgressEvent.failed(model, callbackContext,
                        HandlerErrorCode.NotFound,
                        "The loggingOptions for your account doesn't exist.");
            }

        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        SetV2LoggingOptionsRequest setV2LoggingOptionsRequest = SetV2LoggingOptionsRequest.builder()
                .defaultLogLevel(model.getDefaultLogLevel())
                .roleArn(model.getRoleArn())
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(setV2LoggingOptionsRequest, iotClient::setV2LoggingOptions);
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log(String.format("Update %s [%s] successfully", ResourceModel.TYPE_NAME, model.getAccountId()));

        return ProgressEvent.defaultSuccessHandler(model);
    }
}
