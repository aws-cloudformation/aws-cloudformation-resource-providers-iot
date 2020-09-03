package com.amazonaws.iot.domainconfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationResponse;
import software.amazon.awssdk.services.iot.model.DomainType;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceType;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends DomainConfigurationTestBase {
    private ReadHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new ReadHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(DescribeDomainConfigurationResponse.builder()
                .domainConfigurationName(DOMAIN_CONFIG_NAME)
                .domainConfigurationArn(DOMAIN_CONFIG_ARN)
                .domainName(DOMAIN_NAME)
                .authorizerConfig(AUTHORIZER_CONFIG)
                .domainConfigurationStatus("ENABLED")
                .serverCertificates(SERVER_CERTIFICATE_SUMMARY)
                .domainType(DomainType.ENDPOINT)
                .serviceType(ServiceType.CREDENTIAL_PROVIDER)
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalFailureException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ResourceNotFoundException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build()).when(proxy).injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, null, logger));
    }
}
