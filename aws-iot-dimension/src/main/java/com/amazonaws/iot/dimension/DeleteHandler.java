package com.amazonaws.iot.dimension;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteDimensionRequest;
import software.amazon.awssdk.services.iot.model.DescribeDimensionRequest;
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
        // DeleteDimension API is idempotent, so we have to call Describe first.
        DescribeDimensionRequest describeRequest = DescribeDimensionRequest.builder()
                .name(model.getName())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(describeRequest, iotClient::describeDimension);
        } catch (IotException e) {
            // If the resource doesn't exist, DescribeDimension will throw NotFoundException,
            // which we'll rethrow as CfnNotFoundException - that's all we need to do.
            throw Translator.translateIotExceptionToCfn(e);
        }
        logger.log(String.format("Called Describe for %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, model.getName(), request.getAwsAccountId()));

        DeleteDimensionRequest deleteRequest = DeleteDimensionRequest.builder()
                .name(model.getName())
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotClient::deleteDimension);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        logger.log(String.format("Deleted %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, model.getName(), request.getAwsAccountId()));

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
