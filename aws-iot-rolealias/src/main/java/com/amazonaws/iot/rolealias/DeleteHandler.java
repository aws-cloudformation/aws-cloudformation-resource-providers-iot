package com.amazonaws.iot.rolealias;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DeleteRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeletePolicy";
    private static final String CALL_GRAPH = "AWS-IoT-RoleAlias::Delete";
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        if (model.getRoleAlias() == null) {
            return ProgressEvent.defaultFailureHandler(new Exception("Role Alias must be specified"), HandlerErrorCode.NotFound);
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.defaultSuccessHandler(null))
                );

    }


    private Boolean stabilizedOnDelete(DeleteRoleAliasRequest request, DeleteRoleAliasResponse response, ProxyClient<IotClient> proxyClient, ResourceModel model, CallbackContext callbackContext) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeRoleAlias);
            return false;
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("%s [%s] deletion has stabilized.", ResourceModel.TYPE_NAME, request.roleAlias()));
            return true;
        }
    }

    private DeleteRoleAliasResponse deleteResource(DeleteRoleAliasRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            DeleteRoleAliasResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deleteRoleAlias);
            logger.log(String.format("%s [%s] successfully deleted.", ResourceModel.TYPE_NAME, request.roleAlias()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, OPERATION, request.roleAlias());
        }
    }
}
