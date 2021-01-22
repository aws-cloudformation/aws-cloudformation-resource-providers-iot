package com.amazonaws.iot.domainconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateValidationException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "UpdateDomainConfiguration";

    private IotClient iotClient;

    public UpdateHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public UpdateHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        final ProgressEvent<ResourceModel, CallbackContext> readResponse = (new ReadHandler(iotClient))
                .handleRequest(proxy, request, CallbackContext.builder().build(), logger);

        if (callbackContext != null && callbackContext.isCreateOrUpdateInProgress()) {
            logger.log(String.format("%s [%s] updated successfully", ResourceModel.TYPE_NAME, model.getDomainConfigurationName()));
            return readResponse;
        }

        final ResourceModel prevModel = request.getPreviousResourceState();

        // Determine if we need to set the removeAuthorizerConfig flag by comparing the original state with the
        // desired state.
        boolean removeAuthorizerConfig = false;
        if (prevModel.getAuthorizerConfig() != null && model.getAuthorizerConfig() == null) {
            removeAuthorizerConfig = true;
        }

        final UpdateDomainConfigurationRequest domainRequest = UpdateDomainConfigurationRequest.builder()
                .domainConfigurationName(model.getDomainConfigurationName())
                .authorizerConfig(ResourceUtil.getSdkAuthorizerConfig(model))
                .domainConfigurationStatus(model.getDomainConfigurationStatus())
                .removeAuthorizerConfig(removeAuthorizerConfig)
                .build();

        try {
            UpdateDomainConfigurationResponse response = proxy.injectCredentialsAndInvokeV2(domainRequest,
                    iotClient::updateDomainConfiguration);
            logger.log(String.format("%s [%s] updated. Waiting for %d seconds before returning success.",
                    ResourceModel.TYPE_NAME, model.getDomainConfigurationName(),ResourceUtil.DELAY_CONSTANT));

            return ProgressEvent.defaultInProgressHandler(
                    CallbackContext.builder().createOrUpdateInProgress(true).build(),
                    ResourceUtil.DELAY_CONSTANT,
                    ResourceModel.builder().
                            domainConfigurationName(response.domainConfigurationName())
                            .arn(response.domainConfigurationArn())
                            .build());
        } catch(CertificateValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (IotException e) {
            throw ExceptionTranslator.translateIotExceptionToHandlerException(e, OPERATION, model.getDomainConfigurationName());
        }

    }
}
