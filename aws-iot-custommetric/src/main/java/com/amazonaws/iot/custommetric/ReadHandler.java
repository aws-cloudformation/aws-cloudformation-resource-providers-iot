package com.amazonaws.iot.custommetric;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeCustomMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeCustomMetricResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Set;

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

        DescribeCustomMetricRequest describeRequest = DescribeCustomMetricRequest.builder()
                .metricName(model.getMetricName())
                .build();

        DescribeCustomMetricResponse describeCustomMetricResponse;
        try {
            describeCustomMetricResponse = proxy.injectCredentialsAndInvokeV2(
                    describeRequest, iotClient::describeCustomMetric);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        String metricArn = describeCustomMetricResponse.metricArn();
        logger.log(String.format("Called Describe for %s.", metricArn));

        // Now call ListTagsForResource, because DescribeCustomMetric doesn't provide the tags.
        List<software.amazon.awssdk.services.iot.model.Tag> iotTags = listTags(proxy, metricArn, logger);
        logger.log(String.format("Called ListTags for %s.", metricArn));

        Set<Tag> responseTags = Translator.translateTagsToCfn(iotTags);

        logger.log(String.format("Successfully described %s.", metricArn));

        return ProgressEvent.defaultSuccessHandler(
                ResourceModel.builder()
                        .metricName(describeCustomMetricResponse.metricName())
                        .displayName(describeCustomMetricResponse.displayName())
                        .metricArn(describeCustomMetricResponse.metricArn())
                        .metricType(describeCustomMetricResponse.metricType().toString())
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
