package com.amazonaws.iot.dimension;

import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListDimensionsRequest;
import software.amazon.awssdk.services.iot.model.ListDimensionsResponse;
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

        ListDimensionsRequest listRequest = ListDimensionsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListDimensionsResponse listDimensionsResponse;
        try {
            listDimensionsResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listDimensions);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        List<ResourceModel> models = listDimensionsResponse.dimensionNames().stream()
                .map(dimName -> ResourceModel.builder().name(dimName).build())
                .collect(Collectors.toList());

        logger.log(String.format("Listed %s resources for accountId %s.",
                ResourceModel.TYPE_NAME, request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listDimensionsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
