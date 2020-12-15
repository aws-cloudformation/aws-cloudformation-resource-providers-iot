package com.amazonaws.iot.accountauditconfiguration;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ACCOUNT_ID;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_REQUEST;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.createCfnRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_DescribeShowsV1Config_ExpectSingletonList() {

        ResourceModel model = ResourceModel.builder()
                .accountId("doesn't matter")
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);

        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actualResult.getCallbackContext()).isNull();
        assertThat(actualResult.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(actualResult.getResourceModel()).isNull();
        assertThat(actualResult.getMessage()).isNull();
        assertThat(actualResult.getErrorCode()).isNull();
        assertThat(actualResult.getNextToken()).isNull();
        List<ResourceModel> expectedModels = Collections.singletonList(
                ResourceModel.builder().accountId(ACCOUNT_ID).build());
        assertThat(actualResult.getResourceModels()).isEqualTo(expectedModels);
    }

    @Test
    public void handleRequest_DescribeShowsZeroState_ExpectEmptyList() {

        ResourceModel model = ResourceModel.builder()
                .accountId("doesn't matter")
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(DESCRIBE_RESPONSE_ZERO_STATE);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);

        assertThat(actualResult).isNotNull();
        assertThat(actualResult.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actualResult.getCallbackContext()).isNull();
        assertThat(actualResult.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(actualResult.getResourceModel()).isNull();
        assertThat(actualResult.getMessage()).isNull();
        assertThat(actualResult.getErrorCode()).isNull();
        assertThat(actualResult.getNextToken()).isNull();
        assertThat(actualResult.getResourceModels()).isEmpty();
    }

    @Test
    public void handleRequest_ExceptionFromDescribe_Translated() {

        ResourceModel model = ResourceModel.builder()
                .accountId("doesn't matter")
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenThrow(InternalFailureException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
