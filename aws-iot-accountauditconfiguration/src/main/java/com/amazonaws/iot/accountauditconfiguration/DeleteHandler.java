package com.amazonaws.iot.accountauditconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public DeleteHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        String accountId = request.getAwsAccountId();

        // Call Describe to see whether the configuration was already deleted, as that's
        // what the CFN contracts advise.
        // Note that this API never throws ResourceNotFoundException.
        // If the configuration doesn't exist, it returns an object with all checks disabled,
        // other fields equal to null.
        DescribeAccountAuditConfigurationResponse describeResponse;
        try {
            describeResponse = proxy.injectCredentialsAndInvokeV2(
                    DescribeAccountAuditConfigurationRequest.builder().build(),
                    iotClient::describeAccountAuditConfiguration);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }
        logger.log("Called DescribeAccountAuditConfiguration for " + accountId);

        // We judge whether the configuration exists by the RoleArn field.
        // For an existing configuration, the RoleArn can never be nullified,
        // unless the whole configuration is deleted.
        if (StringUtils.isEmpty(describeResponse.roleArn())) {
            // CFN swallows this NotFound failure, the customer will see success.
            return ProgressEvent.failed(request.getDesiredResourceState(), callbackContext,
                    HandlerErrorCode.NotFound,
                    "The configuration for your account has not been set up or was deleted.");
        }

        try {
            proxy.injectCredentialsAndInvokeV2(
                    DeleteAccountAuditConfigurationRequest.builder().build(),
                    iotClient::deleteAccountAuditConfiguration);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        logger.log("Deleted AccountAuditConfiguration for " + accountId);
        return ProgressEvent.defaultSuccessHandler(null);
    }
}
