package com.amazonaws.iot.mitigationaction;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListMitigationActionsRequest;
import software.amazon.awssdk.services.iot.model.ListMitigationActionsResponse;
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

        ListMitigationActionsRequest listRequest = ListMitigationActionsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListMitigationActionsResponse listMitigationActionsResponse;
        try {
            listMitigationActionsResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listMitigationActions);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        List<ResourceModel> models = listMitigationActionsResponse.actionIdentifiers().stream()
                .map(actionIdentifier -> ResourceModel.builder()
                        .actionName(actionIdentifier.actionName())
                        .mitigationActionArn(actionIdentifier.actionArn())
                        .build())
                .collect(Collectors.toList());

        logger.log(String.format("Listed %s resources for accountId %s.",
                ResourceModel.TYPE_NAME, request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listMitigationActionsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
