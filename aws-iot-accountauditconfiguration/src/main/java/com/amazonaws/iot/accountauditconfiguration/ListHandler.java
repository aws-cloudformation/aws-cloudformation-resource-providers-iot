package com.amazonaws.iot.accountauditconfiguration;

import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ListHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        String accountId = request.getAwsAccountId();

        // There can only be one configuration per account, there's no List API.
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
        List<ResourceModel> models;
        if (StringUtils.isEmpty(describeResponse.roleArn())) {
            models = Collections.emptyList();
        } else {
            ResourceModel model = ResourceModel.builder().accountId(accountId).build();
            models = Collections.singletonList(model);
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
