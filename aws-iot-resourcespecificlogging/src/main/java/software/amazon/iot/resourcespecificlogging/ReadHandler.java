package software.amazon.iot.resourcespecificlogging;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * Read handler function is invoked when detailed information about the resource's current state in a stack is required.
 * 1. Check targetId (primary-identifier) filed is non-null and fetch target type and target name from it.
 * 2. Call ListV2LoggingLevels to fetch a specific log level for a target.
 */
public class ReadHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ReadHandler() {
        this.iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getTargetId())) {
            return ProgressEvent.failed(model, callbackContext,
                    HandlerErrorCode.NotFound,
                    "The targetId for the resource is null or empty.");
        }

        //fetch targetType and targetName from targetId
        String[] targetIdArray = model.getTargetId().split(":", 2);

        String targetType = targetIdArray[0];

        String targetName = targetIdArray[1];

        String logLevel;
        logger.log("Call ListLoggingLevels to get a specific log level for this target.");
        try {
            logLevel = HandlerUtils.getLoggingLevelForTarget(targetType, targetName, proxy, iotClient);

        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        if (logLevel == null) {
            return ProgressEvent.failed(model, callbackContext,
                    HandlerErrorCode.NotFound,
                    "The logLevel for this target doesn't exist.");
        }

        return ProgressEvent.defaultSuccessHandler(ResourceModel.builder()
                .targetId(model.getTargetId())
                .targetType(targetType)
                .targetName(targetName)
                .logLevel(logLevel)
                .build());
    }
}
