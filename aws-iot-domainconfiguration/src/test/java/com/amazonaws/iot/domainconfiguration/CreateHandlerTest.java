package com.amazonaws.iot.domainconfiguration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DomainConfigurationStatus;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
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
public class CreateHandlerTest extends DomainConfigurationTestBase {
    private CreateHandler handler;
    private final ResourceModel expectedResponse = ResourceModel.builder()
            .domainConfigurationName(DOMAIN_CONFIG_NAME)
            .arn(DOMAIN_CONFIG_ARN)
            .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_SimpleCreateInProgress() {
        final ResourceModel model = defaultModelBuilder()
                .domainConfigurationStatus(null)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();


        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DEFAULT_CREATE_DOMAIN_CONFIGURATION_RESPONSE);

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
        assertThat(response.getResourceModel().getDomainConfigurationName()).isEqualTo(DOMAIN_CONFIG_NAME);

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void handleRequest_SimpleCreateSuccess() {
        final ResourceModel model = defaultModelBuilder()
                .domainConfigurationStatus(null)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();


        when(proxy.injectCredentialsAndInvokeV2(any(DescribeDomainConfigurationRequest.class), any()))
                .thenReturn(DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request,
                CallbackContext.builder().createOrUpdateInProgress(true).retryCount(1).build(), logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getDomainConfigurationName()).isEqualTo(DOMAIN_CONFIG_NAME);

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void handleRequest_InvokesTwice() {
        final ResourceModel model = defaultModelBuilder()
                .domainConfigurationStatus(DomainConfigurationStatus.ENABLED.toString())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DEFAULT_CREATE_DOMAIN_CONFIGURATION_RESPONSE);

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
        assertThat(response.getResourceModel().getDomainConfigurationName()).isEqualTo(DOMAIN_CONFIG_NAME);

        verify(proxy, times(2)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void handleRequest_GeneratesName() {
        final ResourceModel model = defaultModelBuilder().domainConfigurationName(null).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(GENERATED_CREATE_DOMAIN_CONFIGURATION_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getResourceModel().getDomainConfigurationName()).isNotNull();
        assertThat(response.getResourceModel().getDomainConfigurationName()).isNotEqualTo(DOMAIN_CONFIG_NAME);
    }

    @Test
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ResourceAlreadyExistsException.builder().resourceId(DOMAIN_CONFIG_NAME).build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_LimitExceededFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(LimitExceededException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
