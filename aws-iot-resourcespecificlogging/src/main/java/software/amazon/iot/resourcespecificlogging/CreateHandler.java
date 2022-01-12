package software.amazon.iot.resourcespecificlogging;

import software.amazon.awssdk.services.iot.model.LogTarget;
import software.amazon.awssdk.services.iot.model.SetV2LoggingLevelRequest;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.iot.IotClient;

/**
 * Create handler function is invoked when there is a cloudformation stack creation operation
 * 1. Build string TragetId to be primary identifier and set it into resource model.
 * 2. Call ListV2LoggingLevels to check if resource already exists.
 * 3. Call SetV2LogingLevel to configure the target-level logging.
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

        //set TargetId in model, we require it as unique primaryIdemtifier
        String targetId = HandlerUtils.targetIdBuilder(model.getTargetType(), model.getTargetName());
        model.setTargetId(targetId);

        //If resource has already existed, throw alreadyExist exception
        String logLevelForTarget;

        logger.log("Call ListLoggingLevels to get a specific log level for this target.");

        try {
            logLevelForTarget = HandlerUtils.getLoggingLevelForTarget(model.getTargetType(), model.getTargetName(), proxy, iotClient);
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }

        if(logLevelForTarget != null) {
            throw new CfnAlreadyExistsException(
                    new Throwable("The V2LoggingLevel for this target already exists."));
        }

        LogTarget logTarget = LogTarget.builder()
            .targetType(model.getTargetType())
            .targetName(model.getTargetName())
            .build();

        SetV2LoggingLevelRequest setV2LoggingLevelRequest = SetV2LoggingLevelRequest.builder()
                .logLevel(model.getLogLevel())
                .logTarget(logTarget)
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(setV2LoggingLevelRequest, iotClient::setV2LoggingLevel);
            logger.log(String.format("Set %s [%s] successfully", ResourceModel.TYPE_NAME, model.getTargetId()));

            return ProgressEvent.defaultSuccessHandler(model);
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(model, e, logger);
        }
    }
}
