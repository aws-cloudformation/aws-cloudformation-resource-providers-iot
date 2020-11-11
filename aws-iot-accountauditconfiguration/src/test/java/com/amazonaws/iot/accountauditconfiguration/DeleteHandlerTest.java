package com.amazonaws.iot.accountauditconfiguration;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_REQUEST;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.createCfnRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

    private static final ResourceModel MODEL_FOR_REQUEST = ResourceModel.builder()
            .accountId("doesn't matter")
            .roleArn("doesn't matter")
            .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        handler = new DeleteHandler();
    }

    @Test
    public void handleRequest_DescribeShowsExistingConfig_VerifyInteractions() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_FOR_REQUEST);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, cfnRequest, null, logger);

        ArgumentCaptor<IotRequest> iotRequestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(iotRequestCaptor.capture(), any());
        assertThat(iotRequestCaptor.getAllValues().get(0))
                .isEqualTo(DescribeAccountAuditConfigurationRequest.builder().build());
        assertThat(iotRequestCaptor.getAllValues().get(1))
                .isEqualTo(DeleteAccountAuditConfigurationRequest.builder().build());

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
    public void handleRequest_DescribeShowsZeroState_ExpectNotFound() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_FOR_REQUEST);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_ZERO_STATE);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        ProgressEvent<ResourceModel, CallbackContext> expectedResult = ProgressEvent.failed(
                MODEL_FOR_REQUEST, null,
                HandlerErrorCode.NotFound,
                "The configuration for your account has not been set up or was deleted.");
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void handleRequest_ExceptionFromDescribe_Translated() {
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_FOR_REQUEST);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenThrow(InternalFailureException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, cfnRequest, null, logger))
                .isInstanceOf(CfnInternalFailureException.class);
    }

    @Test
    public void handleRequest_ExceptionFromDelete_Translated() {
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_FOR_REQUEST);
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE)
                .thenThrow(UnauthorizedException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, cfnRequest, null, logger))
                .isInstanceOf(CfnAccessDeniedException.class);
    }
}
