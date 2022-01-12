package software.amazon.iot.logging;

import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsResponse;
import software.amazon.awssdk.services.iot.model.NotConfiguredException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.awssdk.services.iot.IotClient;

import java.util.Collections;
import java.util.List;

/**
 * List handler function is invoked when there is a request of summary information about multiple resources of this resource type
 * Call GetV2LoggingOption to list this singleton resource.
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

        List<ResourceModel> models;

        String accountId = request.getAwsAccountId();

        // There can only be one loggingOptions resource per account, there's no List API.
        GetV2LoggingOptionsRequest getV2LoggingOptionsRequest = GetV2LoggingOptionsRequest.builder().build();

        try {
            GetV2LoggingOptionsResponse response = proxy.injectCredentialsAndInvokeV2(getV2LoggingOptionsRequest, iotClient::getV2LoggingOptions);
            logger.log(String.format("Get %s [%s] successfully", ResourceModel.TYPE_NAME, accountId));

            if (response.disableAllLogs()) {
                models = Collections.emptyList();
            } else {
                ResourceModel model = ResourceModel.builder()
                        .defaultLogLevel(response.defaultLogLevelAsString())
                        .roleArn(response.roleArn())
                        .accountId(accountId)
                        .build();

                models = Collections.singletonList(model);
            }
        } catch (final NotConfiguredException e) {
            //If the logging option isn't configured return empty list
            logger.log("The account hasn't configured logging options, return empty list.");
            models = Collections.emptyList();
        } catch (RuntimeException e) {
            return ExceptionTranslator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
