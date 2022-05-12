package com.amazonaws.iot.cacertificate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;

import software.amazon.awssdk.services.iot.model.ListCaCertificatesRequest;
import software.amazon.awssdk.services.iot.model.ListCaCertificatesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandlerStd{
    private static final String OPERATION = "ListCACertificates";

    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            ProxyClient<IotClient> proxyClient,
            final Logger logger) {
        this.logger = logger;


        final ListCaCertificatesRequest caCertificatesRequest = ListCaCertificatesRequest.builder()
                .pageSize(50)
                .ascendingOrder(true)
                .marker(request.getNextToken())
                .build();

        try {
            final ListCaCertificatesResponse response = proxy.injectCredentialsAndInvokeV2(
                    caCertificatesRequest,
                    proxyClient.client()::listCACertificates);

            final List<ResourceModel> models = response.certificates().stream()
                    .map(cert-> ResourceModel.builder()
                            .id(cert.certificateId())
                            .arn(cert.certificateArn())
                            .status(cert.statusAsString())
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
