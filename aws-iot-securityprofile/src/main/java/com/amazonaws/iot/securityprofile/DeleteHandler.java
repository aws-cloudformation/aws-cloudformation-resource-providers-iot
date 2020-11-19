package com.amazonaws.iot.securityprofile;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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

        ResourceModel model = request.getDesiredResourceState();

        // From https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
        // "A delete handler MUST return FAILED with a NotFound error code if the
        // resource did not exist prior to the delete request."
        // DeleteSecurityProfile API is idempotent, so we have to call Describe first.
        DescribeSecurityProfileRequest describeRequest = DescribeSecurityProfileRequest.builder()
                .securityProfileName(model.getSecurityProfileName())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(describeRequest, iotClient::describeSecurityProfile);
        } catch (IotException e) {
            // If the resource doesn't exist, DescribeSecurityProfile will throw NotFoundException,
            // which we'll rethrow as CfnNotFoundException - that's all we need to do.
            // CFN (the caller) will swallow this NotFound exception and the customer will see success.
            throw Translator.translateIotExceptionToCfn(e);
        }
        logger.log(String.format("Called Describe for %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, model.getSecurityProfileName(), request.getAwsAccountId()));

        DeleteSecurityProfileRequest deleteRequest = DeleteSecurityProfileRequest.builder()
                .securityProfileName(model.getSecurityProfileName())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotClient::deleteSecurityProfile);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        logger.log(String.format("Deleted %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, model.getSecurityProfileName(), request.getAwsAccountId()));

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
