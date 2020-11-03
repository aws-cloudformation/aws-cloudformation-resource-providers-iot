package com.amazonaws.iot.topicrule;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListTopicRulesRequest;
import software.amazon.awssdk.services.iot.model.ListTopicRulesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {
    private static final int MAX_RESULTS = 50;
    private static final String OPERATION = "ListTopicRules";
    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ListTopicRulesRequest awsRequest = Translator.translateToListRequest(request.getNextToken(), MAX_RESULTS);

        try {
            ListTopicRulesResponse awsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest,
                    proxyClient.client()::listTopicRules);
            final List<ResourceModel> models = Translator.translateFromListResponse(awsResponse);
            String nextToken = awsResponse.nextToken();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(null, OPERATION, e);
        }

    }
}
