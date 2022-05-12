package com.amazonaws.iot.cacertificate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CACertificateStatus;
import software.amazon.awssdk.services.iot.model.DeleteCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DeleteCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UpdateCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.UpdateCaCertificateResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeleteCACertificate";
    private static final String CALL_GRAPH = "AWS-IoT-CACertificate::Delete";

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
            return ProgressEvent.defaultFailureHandler(new Exception("Certificate ID must be specified"), HandlerErrorCode.NotFound);
        }
        model.setStatus(CACertificateStatus.INACTIVE.toString());


        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH + "::CACertificateDeactivation", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .makeServiceCall(this::deactivateCaCertificate)
                                .progress())
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.defaultSuccessHandler(null))
                );
    }

    private Boolean stabilizedOnDelete(DeleteCaCertificateRequest request, DeleteCaCertificateResponse response, ProxyClient<IotClient> proxyClient, ResourceModel model, CallbackContext callbackContext) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeCACertificate);
            return false;
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("%s [%s] deletion has stabilized.", ResourceModel.TYPE_NAME, request.certificateId()));
            return true;
        }
    }

    private DeleteCaCertificateResponse deleteResource(DeleteCaCertificateRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            DeleteCaCertificateResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deleteCACertificate);
            logger.log(String.format("%s [%s] successfully deleted.", ResourceModel.TYPE_NAME, request.certificateId()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, OPERATION, request.certificateId());
        }
    }

    private UpdateCaCertificateResponse deactivateCaCertificate(UpdateCaCertificateRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            UpdateCaCertificateResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::updateCACertificate);
            logger.log(String.format("%s [%s] successfully deactivated before deletion.", ResourceModel.TYPE_NAME, request.certificateId()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, "UpdateCACertificate", request.certificateId());
        }
    }
}
