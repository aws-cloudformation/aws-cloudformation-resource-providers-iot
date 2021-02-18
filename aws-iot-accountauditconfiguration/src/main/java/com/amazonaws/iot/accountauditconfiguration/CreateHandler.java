package com.amazonaws.iot.accountauditconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.UpdateAccountAuditConfigurationRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public CreateHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // We ask customers to specify the Account ID as part of the model,
        // because there must be a primary identifier.
        // Having it in the model helps CFN prevent cases like 2 stacks from managing the same resource.
        String accountIdFromTemplate = model.getAccountId();
        String accountId = request.getAwsAccountId();
        if (!accountIdFromTemplate.equals(accountId)) {
            String message = String.format("AccountId in the template (%s) doesn't match actual: %s.",
                    accountIdFromTemplate, accountId);
            logger.log(message);
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest, message);
        }

        // Call Describe to see whether the customer has a configuration already.
        // Note that this API never throws ResourceNotFoundException.
        // If the configuration doesn't exist, it returns an object with all checks disabled,
        // other fields equal to null.
        DescribeAccountAuditConfigurationResponse describeResponse;
        try {
            describeResponse = proxy.injectCredentialsAndInvokeV2(
                    DescribeAccountAuditConfigurationRequest.builder().build(),
                    iotClient::describeAccountAuditConfiguration);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }
        logger.log("Called DescribeAccountAuditConfiguration for " + accountId);

        // We judge whether the configuration exists by the RoleArn field.
        // A customer cannot create a configuration without specifying a RoleArn.
        // For an existing configuration, the RoleArn can never be nullified,
        // unless the whole configuration is deleted.
        boolean roleArnAlreadyExists = !StringUtils.isEmpty(describeResponse.roleArn());
        if (roleArnAlreadyExists) {
            throw new CfnAlreadyExistsException(
                    new Throwable("The AccountAuditConfiguration already exists."));
        }
        logger.log("DescribeAccountAuditConfiguration for " + accountId +
                " returned a blank config, updating now.");

        // Note that the handlers act as pass-through in terms of input validation.
        // We have some validations in the json model, but we delegate deeper checks to the service.
        // If there's invalid input (e.g. non-existent check name), we'll translate the service's
        // InvalidRequestException and include a readable message.
        UpdateAccountAuditConfigurationRequest updateRequest = UpdateAccountAuditConfigurationRequest.builder()
                .auditCheckConfigurations(Translator.translateChecksFromCfnToIot(model))
                .auditNotificationTargetConfigurationsWithStrings(Translator.translateNotificationsFromCfnToIot(model))
                .roleArn(model.getRoleArn())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(
                    updateRequest, iotClient::updateAccountAuditConfiguration);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log("Created AccountAuditConfiguration for " + accountId);
        return ProgressEvent.defaultSuccessHandler(model);
    }
}
