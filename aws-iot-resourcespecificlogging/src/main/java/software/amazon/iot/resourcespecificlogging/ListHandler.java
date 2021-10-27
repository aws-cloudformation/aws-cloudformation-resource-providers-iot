package software.amazon.iot.resourcespecificlogging;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsRequest;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsResponse;
import software.amazon.awssdk.services.iot.model.LogTargetConfiguration;
import software.amazon.awssdk.services.iot.model.NotConfiguredException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * List handler function is invoked when there is a request of summary information about multiple resources of this resource type
 * Call ListV2LoggingLevels to list all resource-specific logging levels.
 * return an empty list if resource doesn't exists
 */
public class ListHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ListHandler() {
        this.iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ListV2LoggingLevelsRequest listV2LoggingLevelsRequest = ListV2LoggingLevelsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        String accountId = request.getAwsAccountId();
        String nextToken = null;

        ListV2LoggingLevelsResponse response = null;
        try {
            response = proxy.injectCredentialsAndInvokeV2(listV2LoggingLevelsRequest, iotClient::listV2LoggingLevels);
            logger.log(String.format("List %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));
            nextToken = response.nextToken();

        } catch (NotConfiguredException e) {
            //we only log here as we'll return empty list
            logger.log(String.format("The general logging option is not set for this account: %s", accountId));
        } catch (RuntimeException e) {

            return ExceptionTranslator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        List<ResourceModel> models;

        if(response != null && response.hasLogTargetConfigurations()) {
            /**
             * We specify the target name as primary identifier so it should be no-null value,
             * Cloudformation resource won't allow default targetType because there's no targetName associate with it.
             */
            models = response.logTargetConfigurations().stream().filter(t -> isNotDefaultType(t)).map(t -> buildLoggingLevelModel(t)).collect(Collectors.toList());
        } else {
            models = Collections.emptyList();
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    private boolean isNotDefaultType(LogTargetConfiguration logTargetConfiguration) {
        return !logTargetConfiguration.logTarget().targetTypeAsString().equalsIgnoreCase("DEFAULT");
    }

    private ResourceModel buildLoggingLevelModel(LogTargetConfiguration logTargetConfiguration) {

        return ResourceModel.builder()
                .logLevel(logTargetConfiguration.logLevelAsString())
                .targetType(logTargetConfiguration.logTarget().targetTypeAsString())
                .targetName(logTargetConfiguration.logTarget().targetName())
                .targetId(HandlerUtils.targetIdBuilder(logTargetConfiguration.logTarget().targetTypeAsString(), logTargetConfiguration.logTarget().targetName()))
                .build();
    }
}
