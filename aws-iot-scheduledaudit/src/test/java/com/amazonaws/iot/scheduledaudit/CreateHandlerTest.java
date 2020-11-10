package com.amazonaws.iot.scheduledaudit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateScheduledAuditRequest;
import software.amazon.awssdk.services.iot.model.CreateScheduledAuditResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.iot.scheduledaudit.TestConstants.CLIENT_REQUEST_TOKEN;
import static com.amazonaws.iot.scheduledaudit.TestConstants.DAY_OF_WEEK;
import static com.amazonaws.iot.scheduledaudit.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.scheduledaudit.TestConstants.FREQUENCY;
import static com.amazonaws.iot.scheduledaudit.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_ARN;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_NAME;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SDK_MODEL_TAG;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SDK_SYSTEM_TAG;
import static com.amazonaws.iot.scheduledaudit.TestConstants.TARGET_CHECK_NAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .targetCheckNames(TARGET_CHECK_NAMES)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestScheduledAuditIdentifier")
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(DESIRED_TAGS)
                .build();

        CreateScheduledAuditRequest expectedRequest = CreateScheduledAuditRequest.builder()
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .targetCheckNames(TARGET_CHECK_NAMES)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
                .tags(SDK_MODEL_TAG, SDK_SYSTEM_TAG)
                .build();

        CreateScheduledAuditResponse createApiResponse = CreateScheduledAuditResponse.builder()
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
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
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .targetCheckNames(TARGET_CHECK_NAMES)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ProxyThrowsAlreadyExists_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .targetCheckNames(TARGET_CHECK_NAMES)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestScheduledAuditIdentifier")
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
                .targetCheckNames(TARGET_CHECK_NAMES)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
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

        CreateScheduledAuditResponse createApiResponse = CreateScheduledAuditResponse.builder()
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(createApiResponse);

        handler.handleRequest(proxy, request, null, logger);

        ArgumentCaptor<CreateScheduledAuditRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateScheduledAuditRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        CreateScheduledAuditRequest submittedRequest = requestCaptor.getValue();
        // Can't easily check the randomly generated name. Just making sure it contains part of
        // the logical identifier and the stack name, and some more random characters
        assertThat(submittedRequest.scheduledAuditName()).contains("my-stack");
        assertThat(submittedRequest.scheduledAuditName()).contains("MyRes");
        assertThat(submittedRequest.scheduledAuditName().length() > 20).isTrue();
    }

    // TODO: test system tags when the src code is ready
}
