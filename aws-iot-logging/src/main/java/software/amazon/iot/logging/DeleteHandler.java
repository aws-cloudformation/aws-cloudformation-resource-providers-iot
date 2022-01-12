package software.amazon.iot.logging;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsResponse;
import software.amazon.awssdk.services.iot.model.SetV2LoggingOptionsRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Delete handler function is invoked when there is a cloudformation stack deletion operation
 * 1. Check accountId (primary-identifier) filed is matched with actual account.
 * 2. Call GetV2LoggingOption to check if resource already exists.
 * 3. Call SetV2LogingOption to disable the account-level logging (this is only way we can "delete" resource).
 */
public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public DeleteHandler() {
        this.iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // There is no api for deleting logging Option, we can only disable customer logging when resource is deleted
        String accountIdFromTemplate = model.getAccountId();
        String accountId = request.getAwsAccountId();
        if (!accountIdFromTemplate.equals(accountId)) {
            /**
             * This case can only happen in CreateRollback after caller tried creating a Config with wrong AccountID.
             * Returning HandlerErrorCode.NotFound is the right thing to do - it'll allow CFN to succeed
             * idempotently. Otherwise it'd get stuck trying to delete.
             */
            logger.log("Returning NotFound from DeleteHandler due to account ID mismatch, " + accountIdFromTemplate +
                    " from template instead of real " + accountId);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .build();
        }

        /**
         *  We judge whether the configuration exists by calling GetV2LoggingOptions API.
         *  For an existing configuration, the disable-all-logs field is false.
         */
        GetV2LoggingOptionsResponse getV2LoggingOptionsResponse;
        try {
            getV2LoggingOptionsResponse = proxy.injectCredentialsAndInvokeV2(
                    GetV2LoggingOptionsRequest.builder().build(),
                    iotClient::getV2LoggingOptions);

            logger.log(String.format("Get %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));

            // Throw notFound when disableAllLogs is true as it indicates resource doesn't exist
            if (getV2LoggingOptionsResponse.disableAllLogs()) {
                return ProgressEvent.failed(model, callbackContext,
                        HandlerErrorCode.NotFound,
                        "The loggingOptions for your account doesn't exist.");
            }
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        // Disable customer logging
        SetV2LoggingOptionsRequest setV2LoggingOptionsRequest = SetV2LoggingOptionsRequest.builder()
                .disableAllLogs(true)
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(setV2LoggingOptionsRequest, iotClient::setV2LoggingOptions);

        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log(String.format("Disable %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
