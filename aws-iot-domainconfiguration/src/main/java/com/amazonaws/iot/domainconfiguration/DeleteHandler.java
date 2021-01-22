package com.amazonaws.iot.domainconfiguration;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DomainConfigurationStatus;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {
    private static final String DELETE_OPERATION = "DeleteDomainConfiguration";
    private static final String UPDATE_OPERATION = "UpdateDomainConfiguration";

    private IotClient iotClient;

    public DeleteHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public DeleteHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String domainConfigName = model.getDomainConfigurationName();

        String operation = DELETE_OPERATION;

        final DeleteDomainConfigurationRequest deleteRequest = DeleteDomainConfigurationRequest.builder()
                .domainConfigurationName(domainConfigName)
                .build();
        boolean previouslyDisabled = callbackContext != null && callbackContext.isDomainConfigurationDisabled();
        try {
            if(!previouslyDisabled &&
                    !StringUtils.equals(DomainConfigurationStatus.DISABLED.toString(), model.getDomainConfigurationStatus())) {
                operation = UPDATE_OPERATION;
                final UpdateDomainConfigurationRequest updateRequest = UpdateDomainConfigurationRequest.builder()
                        .domainConfigurationName(model.getDomainConfigurationName())
                        .authorizerConfig(ResourceUtil.getSdkAuthorizerConfig(model))
                        .domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                        .removeAuthorizerConfig(false)
                        .build();
                UpdateDomainConfigurationResponse response = proxy.injectCredentialsAndInvokeV2(updateRequest, iotClient::updateDomainConfiguration);
                logger.log(String.format("%s [%s] set as %s before deleting",
                        ResourceModel.TYPE_NAME, model.getDomainConfigurationName(), DomainConfigurationStatus.DISABLED));

                return ProgressEvent.defaultInProgressHandler(
                        CallbackContext.builder().domainConfigurationDisabled(true).build(),
                        ResourceUtil.DELAY_CONSTANT,
                        ResourceModel.builder()
                                .arn(response.domainConfigurationArn())
                                .domainConfigurationName(response.domainConfigurationName())
                                .build());
            }

            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotClient::deleteDomainConfiguration);
            logger.log(String.format("%s [%s] deleted successfully", ResourceModel.TYPE_NAME, domainConfigName));

        } catch (IotException e) {
            throw ExceptionTranslator.translateIotExceptionToHandlerException(e, operation, domainConfigName);
        }

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
