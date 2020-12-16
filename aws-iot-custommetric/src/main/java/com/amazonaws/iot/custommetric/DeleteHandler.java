package com.amazonaws.iot.custommetric;


import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteCustomMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeCustomMetricRequest;
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
        // DeleteCustomMetric API is idempotent, so we have to call Describe first.
        DescribeCustomMetricRequest describeRequest = DescribeCustomMetricRequest.builder()
                .metricName(model.getMetricName())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(describeRequest, iotClient::describeCustomMetric);
        } catch (Exception e) {
            // If the resource doesn't exist, DescribeCustomMetric will throw ResourceNotFoundException,
            // and we'll return FAILED with HandlerErrorCode.NotFound.
            // CFN (the caller) will swallow the "failure" and the customer will see success.
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }
        logger.log(String.format("Called Describe for %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, model.getMetricName(), request.getAwsAccountId()));

        DeleteCustomMetricRequest deleteRequest = DeleteCustomMetricRequest.builder()
                .metricName(model.getMetricName())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotClient::deleteCustomMetric);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        logger.log(String.format("Deleted %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, model.getMetricName(), request.getAwsAccountId()));

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
