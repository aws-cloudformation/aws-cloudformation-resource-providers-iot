package com.amazonaws.iot.mitigationaction;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ID;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.CLIENT_REQUEST_TOKEN;
import static com.amazonaws.iot.mitigationaction.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_NAME;
import static com.amazonaws.iot.mitigationaction.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_ROLE_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_MODEL_TAG;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_SYSTEM_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateMitigationActionRequest;
import software.amazon.awssdk.services.iot.model.CreateMitigationActionResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceModel model = ResourceModel.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .actionParams(ACTION_PARAMS)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestMitigationActionIdentifier")
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(DESIRED_TAGS)
                .build();

        CreateMitigationActionRequest expectedRequest = CreateMitigationActionRequest.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .actionParams(SDK_ACTION_PARAMS)
                .tags(SDK_MODEL_TAG, SDK_SYSTEM_TAG)
                .build();

        CreateMitigationActionResponse createApiResponse = CreateMitigationActionResponse.builder()
                .actionArn(ACTION_ARN)
                .actionId(ACTION_ID)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest), any()))
                .thenReturn(createApiResponse);

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
                .actionParams(ACTION_PARAMS)
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .mitigationActionArn(ACTION_ARN)
                .mitigationActionId(ACTION_ID)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ProxyThrowsAlreadyExists_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .actionName(MITIGATION_ACTION_NAME)
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .actionParams(ACTION_PARAMS)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestMitigationActionIdentifier")
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(DESIRED_TAGS)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceAlreadyExistsException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnAlreadyExistsException.class);
    }

    @Test
    public void handleRequest_NoName_GeneratedByHandler() {

        ResourceModel model = ResourceModel.builder()
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .actionParams(ACTION_PARAMS)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestActionIdentifier")
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(DESIRED_TAGS)
                .build();

        CreateMitigationActionResponse createApiResponse = CreateMitigationActionResponse.builder()
                .actionId(ACTION_ID)
                .actionArn(ACTION_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(createApiResponse);

        handler.handleRequest(proxy, request, null, logger);

        ArgumentCaptor<CreateMitigationActionRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateMitigationActionRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        CreateMitigationActionRequest submittedRequest = requestCaptor.getValue();
        // Can't easily check the randomly generated name. Just making sure it contains
        // the logical identifier and some more random characters.
        assertThat(submittedRequest.actionName()).contains("TestActionIdentifier");
        assertThat(submittedRequest.actionName().length() > "TestActionIdentifier" .length()).isTrue();
    }

    // TODO: test system tags when the src code is ready
}