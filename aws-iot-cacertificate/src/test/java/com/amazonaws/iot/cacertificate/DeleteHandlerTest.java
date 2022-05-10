package com.amazonaws.iot.cacertificate;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.iot.model.AutoRegistrationStatus;
import software.amazon.awssdk.services.iot.model.CACertificateDescription;
import software.amazon.awssdk.services.iot.model.CACertificateStatus;
import software.amazon.awssdk.services.iot.model.CertificateDescription;
import software.amazon.awssdk.services.iot.model.DeleteCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
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
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
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
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends CACertificateTestBase{
    private final DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .verificationCertificatePem(VERIFICATION_CERT_PEM)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeCACertificate(any(DescribeCaCertificateRequest.class)))
                .thenReturn(DescribeCaCertificateResponse.builder()
                        .certificateDescription(
                                CACertificateDescription.builder()
                                        .status(CACertificateStatus.INACTIVE.toString())
                                        .autoRegistrationStatus(AutoRegistrationStatus.DISABLE.toString())
                                        .certificatePem(CA_CERT_PEM)
                                        .certificateId(CA_CERT_ID)
                                        .certificateArn(CA_CERT_ARN)
                                        .build())
                        .build())
                .thenThrow(ResourceNotFoundException.builder().build());


        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DeactivatesWhenActive() {
        final ResourceModel model = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .verificationCertificatePem(VERIFICATION_CERT_PEM)
                .status(CA_CERT_STATUS_ACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        final ArgumentCaptor<UpdateCaCertificateRequest> updateCaCertificateRequest
                = ArgumentCaptor.forClass(UpdateCaCertificateRequest.class);
        when(iotClient.updateCACertificate(any(UpdateCaCertificateRequest.class)))
                .thenReturn(UpdateCaCertificateResponse.builder().build());

        when(iotClient.describeCACertificate(any(DescribeCaCertificateRequest.class)))
                .thenReturn(DescribeCaCertificateResponse.builder()
                        .certificateDescription(
                                CACertificateDescription.builder()
                                        .status(CACertificateStatus.INACTIVE.toString())
                                        .certificatePem(CA_CERT_PEM)
                                        .certificateId(CA_CERT_ID)
                                        .certificateArn(CA_CERT_ARN)
                                        .build())
                        .build())
                .thenThrow(ResourceNotFoundException.builder().build());

        // Need to call delete twice, because first response is in-progress of update to make certificate status INACTIVE
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        verify(iotClient, atLeastOnce()).updateCACertificate(updateCaCertificateRequest.capture());
        assertThat(updateCaCertificateRequest.getValue().certificateId()).isEqualTo(CA_CERT_ID);
        assertThat(updateCaCertificateRequest.getValue().newStatusAsString()).isEqualTo(CA_CERT_STATUS_INACTIVE);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CfnResourceConflictException() {
        testExceptionThrown(DeleteConflictException.builder().build(), CfnResourceConflictException.class);
    }

    @Test
    public void handleRequest_CfnInternalFailureException() {
        testExceptionThrown(InternalFailureException.builder().build(), CfnServiceInternalErrorException.class);
    }

    @Test
    public void handleRequest_ResourceNotFoundSucceeds() {
        testExceptionThrown(
                ResourceNotFoundException.builder().build(),
                CfnNotFoundException.class);
    }

    @Test
    public void handleRequest_CfnGeneralServiceExceptionUnavailable() {
        testExceptionThrown(
                ServiceUnavailableException.builder().build(),
                CfnGeneralServiceException.class);
    }

    @Test
    public void handleRequest_CfnThrottlingException() {
        testExceptionThrown(
                ThrottlingException.builder().build(),
                CfnThrottlingException.class);
    }

    @Test
    public void handleRequest_CfnAccessDeniedException() {
        testExceptionThrown(
                UnauthorizedException.builder().build(),
                CfnAccessDeniedException.class);
    }

    private void testExceptionThrown(IotException iotException, final Class<? extends BaseHandlerException> cfnException) {
        ResourceModel model = ResourceModel.builder()
                .id(CA_CERT_ID)
                .cACertificatePem(CA_CERT_PEM)
                .verificationCertificatePem(VERIFICATION_CERT_PEM)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(iotException)
                .when(iotClient)
                .deleteCACertificate(any(DeleteCaCertificateRequest.class));

        Assertions.assertThrows(cfnException, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
