package com.amazonaws.iot.rolealias;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "DescribeRoleAlias";
    private static final String CALL_GRAPH = "AWS-IoT-RoleAlias::Read";
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            ProxyClient<IotClient> proxyClient,
            Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();


        if (model.getRoleAlias() == null) {
            return ProgressEvent.defaultFailureHandler(new Exception("Role Alias must be specified"), HandlerErrorCode.NotFound);
        }

        return proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readResource)
                .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(response)));
    }



    private DescribeRoleAliasResponse readResource(DescribeRoleAliasRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            DescribeRoleAliasResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    request,
                    proxyClient.client()::describeRoleAlias);
            logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, request.roleAlias()));
            return response;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.roleAlias()
            );
        }
    }
}
