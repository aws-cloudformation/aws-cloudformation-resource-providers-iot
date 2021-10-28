package software.amazon.iot.logging;


import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsResponse;
import software.amazon.awssdk.services.iot.model.NotConfiguredException;
import software.amazon.awssdk.services.iot.model.SetV2LoggingOptionsRequest;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.iot.IotClient;

/**
 * Create handler function is invoked when there is a cloudformation stack creation operation
 * 1. Check accountId (primary-identifier) filed is matched with actual account.
 * 2. Call GetV2LoggingOption to check if resource already exists.
 * 3. Call SetV2LogingOption to configure the account-level logging.
 */
public class CreateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public CreateHandler() {
        this.iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        /**
         * We ask customers to specify the Account ID as part of the model,
         * because there must be a primary identifier.
         * Having it in the model helps CFN prevent cases like 2 stacks from managing the same resource.
         */
        String accountIdFromTemplate = model.getAccountId();
        String accountId = request.getAwsAccountId();
        if (!accountIdFromTemplate.equals(accountId)) {
            String message = String.format("AccountId in the template (%s) doesn't match actual: %s.",
                    accountIdFromTemplate, accountId);
            logger.log(message);
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, message);
        }

        /**
         *  Call GetV2LoggingOption to see whether the customer has a configuration already.
         *  If the logging option is not configured or disable-all-logs flag is true we consider as resource does not exists
         *  Otherwise we consider resource exists and throw ResourceAlreadyExists Exception
         */
        GetV2LoggingOptionsResponse getV2LoggingOptionsResponse = null;
        try {
            getV2LoggingOptionsResponse = proxy.injectCredentialsAndInvokeV2(
                    GetV2LoggingOptionsRequest.builder().build(),
                    iotClient::getV2LoggingOptions);

            logger.log(String.format("Get %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));

        } catch (NotConfiguredException e) {
            logger.log("The account hasn't configured logging options, creating resource now.");
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        if (getV2LoggingOptionsResponse != null && !getV2LoggingOptionsResponse.disableAllLogs()) {
            throw new CfnAlreadyExistsException(
                    new Throwable("The V2LoggingOptions already exists."));
        }

        final SetV2LoggingOptionsRequest setV2LoggingOptionsRequest = SetV2LoggingOptionsRequest.builder()
                .defaultLogLevel(model.getDefaultLogLevel())
                .roleArn(model.getRoleArn())
                .disableAllLogs(false)
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(setV2LoggingOptionsRequest, iotClient::setV2LoggingOptions);
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log(String.format("Set %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));
        return ProgressEvent.defaultSuccessHandler(model);
    }
}
