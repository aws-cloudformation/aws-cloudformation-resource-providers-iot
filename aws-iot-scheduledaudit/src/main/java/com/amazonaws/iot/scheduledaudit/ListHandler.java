package com.amazonaws.iot.scheduledaudit;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListScheduledAuditsRequest;
import software.amazon.awssdk.services.iot.model.ListScheduledAuditsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

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

        ListScheduledAuditsRequest listRequest = ListScheduledAuditsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListScheduledAuditsResponse listScheduledAuditsResponse;
        try {
            listScheduledAuditsResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listScheduledAudits);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        List<ResourceModel> models = listScheduledAuditsResponse.scheduledAudits().stream()
                .map(scheduledAudits -> ResourceModel.builder()
                        .scheduledAuditName(scheduledAudits.scheduledAuditName())
                        .scheduledAuditArn(scheduledAudits.scheduledAuditArn())
                        .frequency(scheduledAudits.frequencyAsString())
                        .dayOfMonth(scheduledAudits.dayOfMonth())
                        .dayOfWeek(Translator.translateDayOfTheWeekToCfn(scheduledAudits.dayOfWeek()))
                        .build())
                .collect(Collectors.toList());

        logger.log(String.format("Listed %s resources for accountId %s.",
                ResourceModel.TYPE_NAME, request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listScheduledAuditsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
