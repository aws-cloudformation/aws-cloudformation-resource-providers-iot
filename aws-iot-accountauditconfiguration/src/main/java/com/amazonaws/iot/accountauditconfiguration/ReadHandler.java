package com.amazonaws.iot.accountauditconfiguration;

import java.util.Map;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ReadHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        String accountId = request.getAwsAccountId();

        DescribeAccountAuditConfigurationResponse describeResponse;
        try {
            describeResponse = proxy.injectCredentialsAndInvokeV2(
                    DescribeAccountAuditConfigurationRequest.builder().build(),
                    iotClient::describeAccountAuditConfiguration);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }
        logger.log("Called DescribeAccountAuditConfiguration for " + accountId);

        // The Describe API never throws ResourceNotFoundException.
        // If the configuration doesn't exist, it returns an object with all checks disabled,
        // other fields equal to null.
        // We judge whether the configuration exists by the RoleArn field.
        // A customer cannot create a configuration without specifying a RoleArn.
        // For an existing configuration, the RoleArn can never be nullified,
        // unless the whole configuration is deleted.
        if (StringUtils.isEmpty(describeResponse.roleArn())) {
            return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext,
                    HandlerErrorCode.NotFound,
                    "The configuration for your account has not been set up or was deleted.");
        }

        Map<String, AuditCheckConfiguration> auditCheckConfigurationsCfn = Translator.translateChecksFromIotToCfn(
                describeResponse.auditCheckConfigurations());

        Map<String, AuditNotificationTarget> notificationTargetsCfn = Translator.translateNotificationsFromIotToCfn(
                describeResponse.auditNotificationTargetConfigurationsAsStrings());

        ResourceModel model = ResourceModel.builder()
                .accountId(accountId)
                .auditCheckConfigurations(auditCheckConfigurationsCfn)
                .auditNotificationTargetConfigurations(notificationTargetsCfn)
                .roleArn(describeResponse.roleArn())
                .build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
