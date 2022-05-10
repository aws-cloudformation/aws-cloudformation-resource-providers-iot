package com.amazonaws.iot.cacertificate;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.iot.model.AutoRegistrationStatus;
import software.amazon.awssdk.services.iot.model.CACertificateDescription;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.UpdateCaCertificateResponse;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends CACertificateTestBase{

    private final UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel prevModel = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .arn(CA_CERT_ARN)
                .status(CA_CERT_STATUS_ACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_ENABLE)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .arn(CA_CERT_ARN)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();


        when(iotClient.updateCACertificate(any(UpdateCaCertificateRequest.class)))
                .thenReturn(UpdateCaCertificateResponse.builder().build());

        final ArgumentCaptor<DescribeCaCertificateRequest> describeCaCertificateRequest
                = ArgumentCaptor.forClass(DescribeCaCertificateRequest.class);
        when(iotClient.describeCACertificate(any(DescribeCaCertificateRequest.class)))
                .thenReturn(DescribeCaCertificateResponse.builder()
                        .certificateDescription(
                                CACertificateDescription.builder()
                                        .status(CA_CERT_STATUS_INACTIVE)
                                        .autoRegistrationStatus(AutoRegistrationStatus.DISABLE.toString())
                                        .certificatePem(CA_CERT_PEM)
                                        .certificateId(CA_CERT_ID)
                                        .certificateArn(CA_CERT_ARN)
                                        .build())
                        .build());


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        verify(iotClient).describeCACertificate(describeCaCertificateRequest.capture());
        assertThat(describeCaCertificateRequest.getValue().certificateId()).isEqualTo(CA_CERT_ID);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getId()).isEqualTo(CA_CERT_ID);
        assertThat(response.getResourceModel().getArn()).isEqualTo(CA_CERT_ARN);
        assertThat(response.getResourceModel().getCACertificatePem()).isEqualTo(CA_CERT_PEM);
        assertThat(response.getResourceModel().getStatus()).isEqualTo(CA_CERT_STATUS_INACTIVE);
        assertThat(response.getResourceModel().getAutoRegistrationStatus()).isEqualTo(CA_CERT_AUTO_REGISTRATION_DISABLE);
    }


    @Test
    public void handleRequest_UpdateRegistrationConfig() {
        final RegistrationConfig preRegistrationConfig = RegistrationConfig.builder()
                .templateBody(TEMPLATE_BODY)
                .roleArn(ROLE_ARN)
                .build();

        final ResourceModel prevModel = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .arn(CA_CERT_ARN)
                .status(CA_CERT_STATUS_ACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_ENABLE)
                .registrationConfig(preRegistrationConfig)
                .build();

        final RegistrationConfig newRegistrationConfig = RegistrationConfig.builder()
                .templateBody(TEMPLATE_BODY)
                .roleArn(UPDATE_ROLE_ARN)
                .build();

        final ResourceModel newModel = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .arn(CA_CERT_ARN)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .registrationConfig(newRegistrationConfig)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();


        when(iotClient.updateCACertificate(any(UpdateCaCertificateRequest.class)))
                .thenReturn(UpdateCaCertificateResponse.builder().build());

        final ArgumentCaptor<DescribeCaCertificateRequest> describeCaCertificateRequest
                = ArgumentCaptor.forClass(DescribeCaCertificateRequest.class);
        when(iotClient.describeCACertificate(any(DescribeCaCertificateRequest.class)))
                .thenReturn(DescribeCaCertificateResponse.builder()
                        .certificateDescription(
                                CACertificateDescription.builder()
                                        .status(CA_CERT_STATUS_INACTIVE)
                                        .autoRegistrationStatus(AutoRegistrationStatus.DISABLE.toString())
                                        .certificatePem(CA_CERT_PEM)
                                        .certificateId(CA_CERT_ID)
                                        .certificateArn(CA_CERT_ARN)
                                        .build())
                        .build());


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        verify(iotClient).describeCACertificate(describeCaCertificateRequest.capture());
        assertThat(describeCaCertificateRequest.getValue().certificateId()).isEqualTo(CA_CERT_ID);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getId()).isEqualTo(CA_CERT_ID);
        assertThat(response.getResourceModel().getArn()).isEqualTo(CA_CERT_ARN);
        assertThat(response.getResourceModel().getCACertificatePem()).isEqualTo(CA_CERT_PEM);
        assertThat(response.getResourceModel().getStatus()).isEqualTo(CA_CERT_STATUS_INACTIVE);
        assertThat(response.getResourceModel().getAutoRegistrationStatus()).isEqualTo(CA_CERT_AUTO_REGISTRATION_DISABLE);
    }

    @Test
    public void handleRequest_failsInternalFailureException() {
        testExceptionThrown(InternalFailureException.builder().build(), CfnServiceInternalErrorException.class);
    }

    @Test
    public void handleRequest_failsInvalidRequestException() {
        testExceptionThrown(
                InvalidRequestException.builder().build(),
                CfnInvalidRequestException.class);
    }

    @Test
    public void handleRequest_failsResourceNotFoundException() {
        testExceptionThrown(
                ResourceNotFoundException.builder().build(),
                CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_failsServiceUnavailableException() {
        testExceptionThrown(
                ServiceUnavailableException.builder().build(),
                CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_failsThrottlingException() {
        testExceptionThrown(
                ThrottlingException.builder().build(),
                CfnThrottlingException.class);
    }

    @Test
    public void handleRequest_failsUnauthorizedException() {
        testExceptionThrown(
                UnauthorizedException.builder().build(),
                CfnAccessDeniedException.class);
    }


    private void testExceptionThrown(IotException iotException, final Class<? extends BaseHandlerException> cfnException) {
        final ResourceModel prevModel = ResourceModel.builder()
                .id(CA_CERT_ID)
                .status(CA_CERT_STATUS_ACTIVE)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .id(CA_CERT_ID)
                .status(CA_CERT_STATUS_INACTIVE)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        doThrow(iotException)
                .when(iotClient)
                .updateCACertificate(any(UpdateCaCertificateRequest.class));

        Assertions.assertThrows(cfnException, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
