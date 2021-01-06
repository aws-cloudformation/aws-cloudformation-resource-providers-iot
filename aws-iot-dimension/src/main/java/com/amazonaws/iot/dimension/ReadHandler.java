package com.amazonaws.iot.dimension;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeDimensionRequest;
import software.amazon.awssdk.services.iot.model.DescribeDimensionResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
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

        ResourceModel model = request.getDesiredResourceState();

        DescribeDimensionRequest describeRequest = DescribeDimensionRequest.builder()
                .name(model.getName())
                .build();

        DescribeDimensionResponse describeDimensionResponse;
        try {
            describeDimensionResponse = proxy.injectCredentialsAndInvokeV2(
                    describeRequest, iotClient::describeDimension);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToErrorCode(model, e, logger);
        }

        String dimensionArn = describeDimensionResponse.arn();
        logger.log(String.format("Called Describe for %s.", dimensionArn));

        // Now call ListTagsForResource, because DescribeDimension doesn't provide the tags.
        List<software.amazon.awssdk.services.iot.model.Tag> iotTags;
        try {
            iotTags = listTags(proxy, dimensionArn, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToErrorCode(model, e, logger);
        }
        logger.log(String.format("Called ListTags for %s.", dimensionArn));

        Set<Tag> responseTags = Translator.translateTagsToCfn(iotTags);

        logger.log(String.format("Successfully described %s.", dimensionArn));

        return ProgressEvent.defaultSuccessHandler(
                ResourceModel.builder()
                        .name(describeDimensionResponse.name())
                        .type(describeDimensionResponse.type().name())
                        .stringValues(new HashSet<>(describeDimensionResponse.stringValues()))
                        .arn(describeDimensionResponse.arn())
                        .tags(responseTags)
                        .build());
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    List<software.amazon.awssdk.services.iot.model.Tag> listTags(AmazonWebServicesClientProxy proxy,
                                                                 String resourceArn, Logger logger) {
        return HandlerUtils.listTags(iotClient, proxy, resourceArn, logger);
    }
}
