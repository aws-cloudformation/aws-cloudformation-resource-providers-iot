package com.amazonaws.iot.rolealias;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListRoleAliasesRequest;
import software.amazon.awssdk.services.iot.model.ListRoleAliasesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd{
    private static final String OPERATION = "ListRoleAliases";

    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            ProxyClient<IotClient> proxyClient,
            final Logger logger) {
        this.logger = logger;

        final ListRoleAliasesRequest roleAliasesRequest = ListRoleAliasesRequest.builder()
                .pageSize(50)
                .ascendingOrder(true)
                .marker(request.getNextToken())
                .build();

        try {
            final ListRoleAliasesResponse response = proxy.injectCredentialsAndInvokeV2(
                    roleAliasesRequest,
                    proxyClient.client()::listRoleAliases);

            final List<ResourceModel> models = response.roleAliases().stream()
                    .map(ra-> ResourceModel.builder()
                            .roleAlias(ra)
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
