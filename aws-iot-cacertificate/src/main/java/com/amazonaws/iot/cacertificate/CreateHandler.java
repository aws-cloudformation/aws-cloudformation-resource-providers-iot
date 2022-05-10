package com.amazonaws.iot.cacertificate;

import com.amazonaws.Response;
import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.IotResponse;
import software.amazon.awssdk.services.iot.model.RegisterCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.RegisterCaCertificateResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.BiFunction;

public class CreateHandler extends BaseHandlerStd {

    private static final String OPERATION = "RegisterCACertificate";
    private static final String CALL_GRAPH = "AWS-IoT-CACertificate::Create";

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

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createResource)
                                .stabilize(this::stabilized)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param request     the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private RegisterCaCertificateResponse createResource(final RegisterCaCertificateRequest request, final ProxyClient<IotClient> proxyClient) {
        try {
            RegisterCaCertificateResponse response = proxyClient.injectCredentialsAndInvokeV2(request,
                    proxyClient.client()::registerCACertificate);
            logger.log(String.format("%s [%s] successfully created.", ResourceModel.TYPE_NAME, response.certificateId()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.caCertificate());
        }
    }


    private Boolean stabilized(RegisterCaCertificateRequest request, RegisterCaCertificateResponse response, ProxyClient<IotClient> proxyClient, ResourceModel model, CallbackContext callbackContext) {
        try {
            if (!StringUtils.isNullOrEmpty(response.certificateId())) {
                model.setId(response.certificateId());
                return true;
            }
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, "RegisterCACertificate", model.getId());
        }
        return false;
    }

}
