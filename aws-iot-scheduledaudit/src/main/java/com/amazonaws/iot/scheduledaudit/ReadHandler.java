package com.amazonaws.iot.scheduledaudit;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeScheduledAuditRequest;
import software.amazon.awssdk.services.iot.model.DescribeScheduledAuditResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
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

        DescribeScheduledAuditRequest describeRequest = DescribeScheduledAuditRequest.builder()
                .scheduledAuditName(model.getScheduledAuditName())
                .build();

        DescribeScheduledAuditResponse describeScheduledAuditResponse;
        try {
            describeScheduledAuditResponse = proxy.injectCredentialsAndInvokeV2(
                    describeRequest, iotClient::describeScheduledAudit);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        String scheduledAuditArn = describeScheduledAuditResponse.scheduledAuditArn();
        logger.log(String.format("Called Describe for %s.", scheduledAuditArn));

        // Now call ListTagsForResource, because DescribeScheduledAudit doesn't provide the tags.
        List<software.amazon.awssdk.services.iot.model.Tag> iotTags = listTags(proxy, scheduledAuditArn, logger);
        logger.log(String.format("Called ListTags for %s.", scheduledAuditArn));

        Set<Tag> responseTags = Translator.translateTagsToCfn(iotTags);

        logger.log(String.format("Successfully described %s.", scheduledAuditArn));

        return ProgressEvent.defaultSuccessHandler(
                ResourceModel.builder()
                        .scheduledAuditName(describeScheduledAuditResponse.scheduledAuditName())
                        .scheduledAuditArn(describeScheduledAuditResponse.scheduledAuditArn())
                        .frequency(describeScheduledAuditResponse.frequencyAsString())
                        .dayOfMonth(describeScheduledAuditResponse.dayOfMonth())
                        .dayOfWeek(Translator.translateDayOfTheWeekToCfn(describeScheduledAuditResponse.dayOfWeek()))
                        .targetCheckNames(new HashSet<>(describeScheduledAuditResponse.targetCheckNames()))
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
