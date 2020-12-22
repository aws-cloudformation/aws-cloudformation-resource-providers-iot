package com.amazonaws.iot.mitigationaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeMitigationActionRequest;
import software.amazon.awssdk.services.iot.model.DescribeMitigationActionResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ID;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_NAME;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_ROLE_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_MODEL_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private ReadHandler handler;

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceModel model = ResourceModel.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DescribeMitigationActionRequest expectedDescribeRequest = DescribeMitigationActionRequest.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .build();
        DescribeMitigationActionResponse describeResponse = DescribeMitigationActionResponse.builder()
                .actionArn(ACTION_ARN)
                .actionId(ACTION_ID)
                .actionName(MITIGATION_ACTION_NAME)
                .actionParams(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS))
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        doReturn(Collections.singletonList(SDK_MODEL_TAG))
                .when(handler)
                .listTags(proxy, ACTION_ARN, logger);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel expectedModel = ResourceModel.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .mitigationActionArn(ACTION_ARN)
                .mitigationActionId(ACTION_ID)
                .actionParams(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ThrowThrottling_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ThrottlingException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }
}
