package com.amazonaws.iot.domainconfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CertificateValidationException;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DomainConfigurationStatus;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends DomainConfigurationTestBase {
    private UpdateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new UpdateHandler();
    }

    @Test
    public void handleRequest_SimpleInProgress() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                .serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] instanceof UpdateDomainConfigurationRequest)
                return DEFAULT_UPDATE_DOMAIN_CONFIGURATION_RESPONSE;
            else if (invocationOnMock.getArguments()[0] instanceof DescribeDomainConfigurationRequest)
                return DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE;
            return null;
        });

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(ResourceUtil.DELAY_CONSTANT);
        assertThat(response.getResourceModel()).isEqualTo(DEFAULT_RESOURCE_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString()).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        when(proxy.injectCredentialsAndInvokeV2(any(DescribeDomainConfigurationRequest.class), any()))
                .thenReturn(DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                CallbackContext.builder().createOrUpdateInProgress(true).build(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(defaultModelBuilder().build());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                .serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        doThrow(ResourceNotFoundException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                .serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] instanceof UpdateDomainConfigurationRequest)
                throw InvalidRequestException.builder().build();
            else if (invocationOnMock.getArguments()[0] instanceof DescribeDomainConfigurationRequest)
                return DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE;
            return null;
        });

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CertificateValidation() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                .serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] instanceof UpdateDomainConfigurationRequest)
                throw CertificateValidationException.builder().build();
            else if (invocationOnMock.getArguments()[0] instanceof DescribeDomainConfigurationRequest)
                return DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE;
            return null;
        });

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_Throttling() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                .serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] instanceof UpdateDomainConfigurationRequest)
                throw ThrottlingException.builder().build();
            else if (invocationOnMock.getArguments()[0] instanceof DescribeDomainConfigurationRequest)
                return DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE;
            return null;
        });

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceModel model = defaultModelBuilder().serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceModel prevModel = defaultModelBuilder().domainConfigurationStatus(DomainConfigurationStatus.DISABLED.toString())
                .serverCertificateArns(Collections.singletonList(SERVER_CERT_ARN)).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).previousResourceState(prevModel).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenAnswer(invocationOnMock -> {
            if (invocationOnMock.getArguments()[0] instanceof UpdateDomainConfigurationRequest)
                throw InternalFailureException.builder().build();
            else if (invocationOnMock.getArguments()[0] instanceof DescribeDomainConfigurationRequest)
                return DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE;
            return null;
        });

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
