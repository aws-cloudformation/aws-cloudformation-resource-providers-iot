package com.amazonaws.iot.policy;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPoliciesRequest;
import software.amazon.awssdk.services.iot.model.ListPoliciesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd{
    private static final String OPERATION = "ListPolicies";

    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            ProxyClient<IotClient> proxyClient,
            final Logger logger) {
        this.logger = logger;

        final ListPoliciesRequest policiesRequestRequest = ListPoliciesRequest.builder()
                .pageSize(50)
                .marker(request.getNextToken())
                .build();

        try {
            final ListPoliciesResponse response = proxy.injectCredentialsAndInvokeV2(
                    policiesRequestRequest,
                    proxyClient.client()::listPolicies);

            final List<ResourceModel> models = response.policies().stream()
                    .map(policy -> ResourceModel.builder()
                            .policyName(policy.policyName())
                            .arn(policy.policyArn())
                            .build())
                    .collect(Collectors.toList());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .nextToken(response.nextMarker())
                    .status(OperationStatus.SUCCESS)
                    .build();

        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, OPERATION, "");
        }
    }
}
