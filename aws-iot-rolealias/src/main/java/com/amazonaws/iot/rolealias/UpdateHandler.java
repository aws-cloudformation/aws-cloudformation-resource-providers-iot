package com.amazonaws.iot.rolealias;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UpdateRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.UpdateRoleAliasResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private static final String OPERATION = "UpdateRoleAlias";
    private static final String CALL_GRAPH = "AWS-IoT-RoleAlias::Update";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel newModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        // Without this check, injectCredentialsAndInvokeV2 will throw software.amazon.awssdk.core.exception.SdkClientException
        // Better to fail quickly and with a clear error here (contract tests required NotFound error code).
        if (prevModel.getRoleAlias() == null || newModel.getRoleAlias() == null) {
            return ProgressEvent.defaultFailureHandler(new Exception("Role Alias must be specified"), HandlerErrorCode.NotFound);
        }

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .makeServiceCall(this::updateResource)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private UpdateRoleAliasResponse updateResource(UpdateRoleAliasRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            UpdateRoleAliasResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::updateRoleAlias);
            logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, request.roleAlias()));
            return  response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.roleAlias());
        }
    }
}
