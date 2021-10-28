package software.amazon.iot.logging;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Read handler function is invoked when detailed information about the resource's current state in a stack is required.
 * Call GetV2LoggingOption to read this singleton resource.
 */
public class ReadHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ReadHandler() {
        iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final GetV2LoggingOptionsRequest getV2LoggingOptionsRequest = GetV2LoggingOptionsRequest.builder().build();

        final ResourceModel model = request.getDesiredResourceState();

        String accountId = request.getAwsAccountId();

        try {
            GetV2LoggingOptionsResponse response = proxy.injectCredentialsAndInvokeV2(getV2LoggingOptionsRequest, iotClient::getV2LoggingOptions);
            logger.log(String.format("Get %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));

            // Throw notFound when disableAllLogs is true as it indicates resource doesn't exist
            if (response.disableAllLogs()) {
                return ProgressEvent.failed(model, callbackContext,
                        HandlerErrorCode.NotFound,
                        "The loggingOptions for your account doesn't exist.");
            }

            return ProgressEvent.defaultSuccessHandler(ResourceModel.builder()
                    .defaultLogLevel(response.defaultLogLevelAsString())
                    .roleArn(response.roleArn())
                    .accountId(accountId)
                    .build());

        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }
    }
}
