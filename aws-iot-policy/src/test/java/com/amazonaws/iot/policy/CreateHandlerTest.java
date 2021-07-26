package com.amazonaws.iot.policy;

import org.junit.jupiter.api.Assertions;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends PolicyTestBase{
    private CreateHandler handler;

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
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = defaultModelBuilder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DEFAULT_CREATE_POLICY_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getId()).isEqualTo(POLICY_ID);
        assertThat(response.getResourceModel().getArn()).isEqualTo(POLICY_ARN);
        assertThat(response.getResourceModel().getPolicyName()).isEqualTo(POLICY_NAME);
        assertThat(response.getResourceModel().getPolicyName()).isEqualTo(POLICY_DOCUMENT);
    }

    @Test
    public void handleRequest_InvokesTwice() {
        final ResourceModel model = defaultModelBuilder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DEFAULT_CREATE_POLICY_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getResourceModel()).isEqualTo(DEFAULT_RESOURCE_MODEL);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getPolicyName()).isEqualTo(POLICY_NAME);

        verify(proxy, times(2)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();
        doThrow(ResourceAlreadyExistsException.builder().resourceId(POLICY_ID).build())
                .when(iotClient)
                .createPolicy(any(CreatePolicyRequest.class));

        Assertions.assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, null, logger));
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request,null, logger));
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
