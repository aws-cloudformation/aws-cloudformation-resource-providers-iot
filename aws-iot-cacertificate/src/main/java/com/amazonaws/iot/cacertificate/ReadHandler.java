package com.amazonaws.iot.cacertificate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "DescribeCACertificate";
    private static final String CALL_GRAPH = "AWS-IoT-CACertificate::Read";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();

        if (model.getId() == null) {
            return ProgressEvent.defaultFailureHandler(new Exception("CA Certificate ID must be specified"), HandlerErrorCode.NotFound);
        }

        return proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readResource)
                .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(response)));
    }

    private DescribeCaCertificateResponse readResource(final DescribeCaCertificateRequest request, final ProxyClient<IotClient> proxyClient) {
        try {
            DescribeCaCertificateResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::describeCACertificate);
            logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, request.certificateId()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, OPERATION, request.certificateId());
        }
    }
}
