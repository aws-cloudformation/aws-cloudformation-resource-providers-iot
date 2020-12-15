package com.amazonaws.iot.mitigationaction;

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
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ID;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.CLIENT_REQUEST_TOKEN;
import static com.amazonaws.iot.mitigationaction.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_NAME;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_ROLE_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_MODEL_TAG;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_SYSTEM_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                .actionParams(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
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
                .actionParams(SDK_ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY)
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
                .actionParams(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
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
                .actionParams(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
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

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_NoName_GeneratedByHandler() {

        ResourceModel model = ResourceModel.builder()
                .roleArn(MITIGATION_ACTION_ROLE_ARN)
                .actionParams(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("MyResourceName")
                .clientRequestToken("MyToken")
                .desiredResourceTags(DESIRED_TAGS)
                .stackId("arn:aws:cloudformation:us-east-1:123456789012:stack/my-stack-name/" +
                        "084c0bd1-082b-11eb-afdc-0a2fadfa68a5")
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

        // Can't easily check the randomly generated name. Just making sure it contains part of
        // the logical identifier and the stack name, and some more random characters
        assertThat(submittedRequest.actionName()).contains("my-stack");
        assertThat(submittedRequest.actionName()).contains("MyRes");
        assertThat(submittedRequest.actionName().length() > 20).isTrue();
    }

    // TODO: test system tags when the src code is ready
}
