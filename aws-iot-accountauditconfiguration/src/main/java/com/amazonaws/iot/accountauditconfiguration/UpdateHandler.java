package com.amazonaws.iot.accountauditconfiguration;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AuditCheckConfiguration;
import software.amazon.awssdk.services.iot.model.AuditNotificationTarget;
import software.amazon.awssdk.services.iot.model.AuditNotificationType;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.UpdateAccountAuditConfigurationRequest;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public UpdateHandler() {
        this.iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // Note: Unlike CreateHandler, there's no need to verify the accountID in the model vs the request.
        // Since it's a primaryIdentifier, upon change, CFN will issue Create+Delete rather than just Update.
        // The Create will fail in that case because it does have the account ID check.
        String accountId = request.getAwsAccountId();

        // We need to call Describe to overwrite out of band updates.
        // For example, a customer can enable a check that this template doesn't mention.
        // The UpdateAccountAuditConfiguration API has a PATCH behavior.
        DescribeAccountAuditConfigurationResponse describeResponse;
        try {
            describeResponse = proxy.injectCredentialsAndInvokeV2(
                    DescribeAccountAuditConfigurationRequest.builder().build(),
                    iotClient::describeAccountAuditConfiguration);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }
        logger.log(String.format("Called DescribeAccountAuditConfiguration for %s.", accountId));

        // If RoleArn is null, the configuration must've been deleted. The RoleArn can never be nullified.
        if (StringUtils.isEmpty(describeResponse.roleArn())) {
            String message = "The configuration for your account has not been set up or was deleted.";
            logger.log(message);
            return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext,
                    HandlerErrorCode.NotFound, message);
        }

        Map<String, AuditCheckConfiguration> checkConfigurationsForUpdate =
                buildCheckConfigurationsForUpdate(model, describeResponse);

        Map<String, AuditNotificationTarget> notificationTargetConfigurationsForUpdate =
                buildNotificationTargetConfigurationsForUpdate(model);

        UpdateAccountAuditConfigurationRequest updateRequest = UpdateAccountAuditConfigurationRequest.builder()
                .auditCheckConfigurations(checkConfigurationsForUpdate)
                .auditNotificationTargetConfigurationsWithStrings(notificationTargetConfigurationsForUpdate)
                .roleArn(model.getRoleArn())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(
                    updateRequest, iotClient::updateAccountAuditConfiguration);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log(String.format("Updated AccountAuditConfiguration for %s.", accountId));
        return ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState());
    }

    Map<String, AuditCheckConfiguration> buildCheckConfigurationsForUpdate(
            ResourceModel model,
            DescribeAccountAuditConfigurationResponse describeResponse) {

        // The UpdateAccountAuditConfiguration API has a PATCH behavior, so we can't just
        // translate the map from the model.
        // A customer can enable a check out of band that this template doesn't mention.
        // We create the map in 2 steps. First, create a map with all supported checks disabled.
        // Second, copy all the entries from the model.
        Map<String, AuditCheckConfiguration> checkConfigurationsForUpdate = new HashMap<>();
        describeResponse.auditCheckConfigurations().keySet().forEach(k ->
                checkConfigurationsForUpdate.put(k, AuditCheckConfiguration.builder().enabled(false).build()));
        model.getAuditCheckConfigurations().forEach((key, value) ->
                checkConfigurationsForUpdate.put(key, Translator.translateCheckConfigurationFromCfnToIot(value)));
        return checkConfigurationsForUpdate;
    }

    Map<String, AuditNotificationTarget> buildNotificationTargetConfigurationsForUpdate(
            ResourceModel model) {

        // The UpdateAccountAuditConfiguration API has a PATCH behavior, so we can't just
        // translate the map from the model.
        // A customer can enable a notification target out of band that this template doesn't mention.
        // We create the map in 2 steps. First, create a map with all supported notification target types.
        // Second, copy all the entries from the model.
        Map<String, AuditNotificationTarget> notificationTargetConfigurationsForUpdate = new HashMap<>();
        AuditNotificationType.knownValues().forEach(type -> notificationTargetConfigurationsForUpdate.put(
                type.toString(), AuditNotificationTarget.builder().enabled(false).build()));
        Translator.translateNotificationsFromCfnToIot(model)
                .forEach(notificationTargetConfigurationsForUpdate::put);
        return notificationTargetConfigurationsForUpdate;
    }

}
