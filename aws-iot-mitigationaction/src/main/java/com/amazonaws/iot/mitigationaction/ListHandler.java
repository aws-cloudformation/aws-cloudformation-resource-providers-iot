package com.amazonaws.iot.mitigationaction;

import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListMitigationActionsRequest;
import software.amazon.awssdk.services.iot.model.ListMitigationActionsResponse;
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

        ListMitigationActionsRequest listRequest = ListMitigationActionsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListMitigationActionsResponse listMitigationActionsResponse;
        try {
            listMitigationActionsResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listMitigationActions);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        List<ResourceModel> models = listMitigationActionsResponse.actionIdentifiers().stream()
                .map(actionIdentifier -> ResourceModel.builder()
                        .actionName(actionIdentifier.actionName())
                        .mitigationActionArn(actionIdentifier.actionArn())
                        .creationDate(actionIdentifier.creationDate().toString())
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