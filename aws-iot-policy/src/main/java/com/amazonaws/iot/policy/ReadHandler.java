package com.amazonaws.iot.policy;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetPolicyRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "GetPolicy";
    private static final String CALL_GRAPH = "AWS-IoT-Policy::Read";
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


        if (model.getPolicyName() == null) {
            model.setPolicyName(model.getId());
        }

        return proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readResource)
                .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(response)));
    }



    private GetPolicyResponse readResource(GetPolicyRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            GetPolicyResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    request,
                    proxyClient.client()::getPolicy);
            logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, request.policyName()));
            return response;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.policyName()
            );
        }
    }
}
