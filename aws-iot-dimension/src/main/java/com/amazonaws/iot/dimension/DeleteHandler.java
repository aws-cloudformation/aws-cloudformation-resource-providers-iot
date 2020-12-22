package com.amazonaws.iot.dimension;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteDimensionRequest;
import software.amazon.awssdk.services.iot.model.DescribeDimensionRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.regex.Pattern;


public class DeleteHandler extends BaseHandler<CallbackContext> {

    private static final Pattern RESOURCE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9:_-]+");

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

        // Before we call Describe, we also need to deal with an InvalidRequest edge case.
        // If CFN is trying to delete a resource with an invalid name, returning InvalidRequest would
        // get CFN stuck in delete-failed state. If we return NotFound, it'll just succeed.
        // We wouldn't have to do this if aws-cloudformation-rpdk-java-plugin had functioning regex
        // pattern evaluation (known issue with an internal ticket).
        String dimensionName = model.getName();
        boolean matches = RESOURCE_NAME_PATTERN.matcher(dimensionName).matches();
        if (!matches) {
            logger.log("Returning NotFound from DeleteHandler due to invalid name " + dimensionName);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .errorCode(HandlerErrorCode.NotFound)
                    .build();
        }

        DescribeDimensionRequest describeRequest = DescribeDimensionRequest.builder()
                .name(dimensionName)
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(describeRequest, iotClient::describeDimension);
        } catch (Exception e) {
            // If the resource doesn't exist, DescribeDimension will throw NotFoundException,
            // which we'll translate to NotFound Failure - that's all we need to do.
            // CFN (the caller) will swallow this failure and the customer will see success.
            return Translator.translateExceptionToErrorCode(model, e, logger);
        }
        logger.log(String.format("Called Describe for %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, dimensionName, request.getAwsAccountId()));

        DeleteDimensionRequest deleteRequest = DeleteDimensionRequest.builder()
                .name(dimensionName)
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotClient::deleteDimension);
        } catch (Exception e) {
            return Translator.translateExceptionToErrorCode(model, e, logger);
        }

        logger.log(String.format("Deleted %s with name %s, accountId %s.",
                ResourceModel.TYPE_NAME, dimensionName, request.getAwsAccountId()));

        return ProgressEvent.defaultSuccessHandler(null);
    }
}
