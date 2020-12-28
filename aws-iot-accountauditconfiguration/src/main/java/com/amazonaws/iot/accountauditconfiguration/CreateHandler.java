package com.amazonaws.iot.accountauditconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AuditNotificationTarget;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.UpdateAccountAuditConfigurationRequest;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;
import java.util.Set;

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
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }
        logger.log("Called DescribeAccountAuditConfiguration for " + accountId);

        // We judge whether the configuration exists by the RoleArn field.
        // A customer cannot create a configuration without specifying a RoleArn.
        // For an existing configuration, the RoleArn can never be nullified,
        // unless the whole configuration is deleted.
        boolean roleArnAlreadyExists = !StringUtils.isEmpty(describeResponse.roleArn());
        if (roleArnAlreadyExists) {
            // Note: we don't fail with AlreadyExists as soon as we see an existing configuration.
            // We return success if the existing configuration is identical.
            // We do this to not break the customer's template in case of a transient exception.
            // Consider a scenario where our CreateHandler succeeds, but our response doesn't reach CFN.
            // CFN would retry. If we returned AlreadyExists, CFN would throw "CREATE_FAILED: resource already exists"
            // to the customer.
            // We do know that this configuration could've been created manually rather than through
            // CFN, but, unlike for other resources, we don't have a way to tell how
            // the configuration was created.
            boolean areEquivalent = areEquivalent(model, describeResponse, logger);
            logger.log("An AccountAuditConfiguration already existed, areEquivalent=" + areEquivalent);
            if (areEquivalent) {
                return ProgressEvent.defaultSuccessHandler(model);
            } else {
                throw new CfnAlreadyExistsException(
                        new Throwable("A configuration with different properties already exists."));
            }
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
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log("Created AccountAuditConfiguration for " + accountId);
        return ProgressEvent.defaultSuccessHandler(model);
    }

    private boolean areEquivalent(
            ResourceModel model,
            DescribeAccountAuditConfigurationResponse describeResponse,
            Logger logger) {

        if (!describeResponse.roleArn().equals(model.getRoleArn())) {
            logger.log("AccountAuditConfiguration already exists with a different role ARN: " +
                    describeResponse.roleArn());
            return false;
        }

        if (!areCheckConfigurationsEquivalent(model, describeResponse)) {
            logger.log("AccountAuditConfiguration already exists with different check configurations enabled.");
            return false;
        }

        if (!areNotificationTargetsEquivalent(model, describeResponse)) {
            logger.log("AccountAuditConfiguration already exists with different notification " +
                    "target configurations enabled.");
            return false;
        }
        return true;
    }

    boolean areCheckConfigurationsEquivalent(
            ResourceModel model,
            DescribeAccountAuditConfigurationResponse describeResponse) {

        // We can't simply compare the request and response maps, because DescribeResponse
        // contains all the available checks in disabled state, even if the customer never touched them.
        Set<String> checksEnabledInDescribeResponse = Translator.getEnabledChecksSetFromIotMap(
                describeResponse.auditCheckConfigurations());

        Set<String> checksEnabledInTemplate = Translator.getEnabledChecksSetFromIotMap(
                Translator.translateChecksFromCfnToIot(model));

        return checksEnabledInDescribeResponse.equals(checksEnabledInTemplate);
    }

    boolean areNotificationTargetsEquivalent(
            ResourceModel model,
            DescribeAccountAuditConfigurationResponse describeResponse) {

        // Unlike CheckConfigurations, the default state for Notifications is null, not disabled.
        // This allows us to simply check if the maps are equal.
        Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
                notificationTargetConfigurationsFromTemplate = Translator.translateNotificationsFromCfnToIot(model);

        Map<String, AuditNotificationTarget> notificationTargetConfigurationsFromDescribe =
                describeResponse.auditNotificationTargetConfigurationsAsStrings();
        if (CollectionUtils.isNullOrEmpty(notificationTargetConfigurationsFromDescribe)) {
            return CollectionUtils.isNullOrEmpty(notificationTargetConfigurationsFromTemplate);
        } else {
            return notificationTargetConfigurationsFromTemplate.equals(
                    notificationTargetConfigurationsFromDescribe);
        }
    }
}
