package com.amazonaws.iot.domainconfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends DomainConfigurationTestBase {
    private DeleteHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new DeleteHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = defaultModelBuilder().domainConfigurationStatus("DISABLED").build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

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
    public void handleRequest_DisableBeforeDelete() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DEFAULT_UPDATE_DOMAIN_CONFIGURATION_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(UpdateDomainConfigurationRequest.class), any());

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
    public void handleRequest_InProgressDeleteSucceeds() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                CallbackContext.builder().domainConfigurationDisabled(true).build(), logger);

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
    public void handleRequest_ResourceNotFoundFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ResourceNotFoundException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CfnInternalFailureException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalFailureException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CfnInvalidRequestException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CfnGeneralServiceExceptionUnavailable() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ServiceUnavailableException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CfnThrottlingException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_CfnAccessDeniedException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(UnauthorizedException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
