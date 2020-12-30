package com.amazonaws.iot.custommetric;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListCustomMetricsRequest;
import software.amazon.awssdk.services.iot.model.ListCustomMetricsResponse;
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

        ListCustomMetricsRequest listRequest = ListCustomMetricsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListCustomMetricsResponse listCustomMetricsResponse;
        try {
            listCustomMetricsResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listCustomMetrics);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        List<ResourceModel> models = listCustomMetricsResponse.metricNames().stream()
                .map(metricName -> ResourceModel.builder()
                        .metricName(metricName)
                        .build())
                .collect(Collectors.toList());

        logger.log(String.format("Listed %s resources for accountId %s.",
                ResourceModel.TYPE_NAME, request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listCustomMetricsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
