package software.amazon.iot.resourcespecificlogging;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.LogTarget;
import software.amazon.awssdk.services.iot.model.SetV2LoggingLevelRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Update handler function is invoked when there is a cloudformation stack update operation
 * 1. Check targetId (primary-identifier) filed is non-null and fetch target type and target name from it.
 * 2. Call ListV2LoggingLevels to check if resource already exists.
 * 3. Call SetV2LogingLevel to reconfigure the target-level logging.
 */
public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public UpdateHandler() {
        this.iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getTargetId())) {
            return ProgressEvent.failed(model, callbackContext,
                    HandlerErrorCode.NotFound,
                    "The targetId for the resource is null or empty.");
        }

        //fetch targetType and targetName from targetId
        String[] targetIdArray = model.getTargetId().split(":", 2);

        String targetType = targetIdArray[0];

        String targetName = targetIdArray[1];

        //If resource has already existed, throw alreadyExist exception
        String logLevelForTarget;

        logger.log("Call ListLoggingLevels to get a specific log level for this target.");

        try {
            logLevelForTarget = HandlerUtils.getLoggingLevelForTarget(targetType, targetName, proxy, iotClient);
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        if (logLevelForTarget == null) {
            return ProgressEvent.failed(model, callbackContext,
                    HandlerErrorCode.NotFound,
                    "The logLevel for this target doesn't exist.");
        }

        //update resource
        software.amazon.awssdk.services.iot.model.LogTarget logTarget = LogTarget.builder()
                .targetType(targetType)
                .targetName(targetName)
                .build();

        SetV2LoggingLevelRequest setV2LoggingLevelRequest = SetV2LoggingLevelRequest.builder()
                .logLevel(model.getLogLevel())
                .logTarget(logTarget)
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(setV2LoggingLevelRequest, iotClient::setV2LoggingLevel);

        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log(String.format("Update %s [%s] successfully", ResourceModel.TYPE_NAME, model.getTargetId()));

        return ProgressEvent.defaultSuccessHandler(model);
    }
}
