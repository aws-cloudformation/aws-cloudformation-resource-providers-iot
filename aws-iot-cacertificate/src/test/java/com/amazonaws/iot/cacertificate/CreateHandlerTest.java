package com.amazonaws.iot.cacertificate;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.iot.model.AutoRegistrationStatus;
import software.amazon.awssdk.services.iot.model.CACertificateDescription;
import software.amazon.awssdk.services.iot.model.CACertificateStatus;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.RegisterCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.RegisterCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends CACertificateTestBase{
    private CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final RegistrationConfig preRegistrationConfig = RegistrationConfig.builder()
                .templateBody(TEMPLATE_BODY)
                .roleArn(ROLE_ARN)
                .build();

        final ResourceModel model = ResourceModel.builder()
                .cACertificatePem(CA_CERT_PEM)
                .verificationCertificatePem(VERIFICATION_CERT_PEM)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .registrationConfig(preRegistrationConfig)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.registerCACertificate(any(RegisterCaCertificateRequest.class)))
                .thenReturn(RegisterCaCertificateResponse.builder()
                        .certificateArn(CA_CERT_ARN)
                        .certificateId(CA_CERT_ID)
                        .build());

        final ArgumentCaptor<DescribeCaCertificateRequest> describeCaCertificateRequest
                = ArgumentCaptor.forClass(DescribeCaCertificateRequest.class);

        when(iotClient.describeCACertificate(any(DescribeCaCertificateRequest.class)))
                .thenReturn(DescribeCaCertificateResponse.builder()
                        .certificateDescription(
                                CACertificateDescription.builder()
                                        .status(CACertificateStatus.INACTIVE)
                                        .autoRegistrationStatus(AutoRegistrationStatus.DISABLE)
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
        assertThat(response.getResourceModel().getStatus()).isEqualTo(CACertificateStatus.INACTIVE.toString());
        assertThat(response.getResourceModel().getAutoRegistrationStatus()).isEqualTo(AutoRegistrationStatus.DISABLE.toString());
    }


    @Test
    public void handleRequest_MissingRegistrationFields() {
        final RegistrationConfig preRegistrationConfig = RegistrationConfig.builder()
                .templateBody(TEMPLATE_BODY)
                .build();

        final ResourceModel model = ResourceModel.builder()
                .cACertificatePem(CA_CERT_PEM)
                .verificationCertificatePem(VERIFICATION_CERT_PEM)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE)
                .registrationConfig(preRegistrationConfig)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.registerCACertificate(any(RegisterCaCertificateRequest.class)))
                .thenReturn(RegisterCaCertificateResponse.builder()
                        .certificateArn(CA_CERT_ARN)
                        .certificateId(CA_CERT_ID)
                        .build());

        final ArgumentCaptor<DescribeCaCertificateRequest> describeCaCertificateRequest
                = ArgumentCaptor.forClass(DescribeCaCertificateRequest.class);

        when(iotClient.describeCACertificate(any(DescribeCaCertificateRequest.class)))
                .thenReturn(DescribeCaCertificateResponse.builder()
                        .certificateDescription(
                                CACertificateDescription.builder()
                                        .status(CACertificateStatus.INACTIVE)
                                        .autoRegistrationStatus(AutoRegistrationStatus.DISABLE)
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
        assertThat(response.getResourceModel().getStatus()).isEqualTo(CACertificateStatus.INACTIVE.toString());
        assertThat(response.getResourceModel().getAutoRegistrationStatus()).isEqualTo(AutoRegistrationStatus.DISABLE.toString());
    }

    @Test
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ResourceAlreadyExistsException.builder().resourceId(CA_CERT_ID).build())
                .when(iotClient)
                .registerCACertificate(any(RegisterCaCertificateRequest.class));

        Assertions.assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .registerCACertificate(any(RegisterCaCertificateRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalException.builder().build())
                .when(iotClient)
                .registerCACertificate(any(RegisterCaCertificateRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .registerCACertificate(any(RegisterCaCertificateRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
