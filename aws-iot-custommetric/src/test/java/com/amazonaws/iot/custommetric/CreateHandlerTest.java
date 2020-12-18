package com.amazonaws.iot.custommetric;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateCustomMetricRequest;
import software.amazon.awssdk.services.iot.model.CreateCustomMetricResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.iot.custommetric.TestConstants.CLIENT_REQUEST_TOKEN;
import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_ARN;
import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_NAME;
import static com.amazonaws.iot.custommetric.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.custommetric.TestConstants.DISPLAY_NAME;
import static com.amazonaws.iot.custommetric.TestConstants.METRIC_TYPE;
import static com.amazonaws.iot.custommetric.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.custommetric.TestConstants.SDK_MODEL_TAG;
import static com.amazonaws.iot.custommetric.TestConstants.SDK_SYSTEM_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceModel model = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestCustomMetricIdentifier")
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(DESIRED_TAGS)
                .build();

        CreateCustomMetricRequest expectedRequest = CreateCustomMetricRequest.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME)
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .tags(SDK_MODEL_TAG, SDK_SYSTEM_TAG)
                .build();

        CreateCustomMetricResponse createApiResponse = CreateCustomMetricResponse.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
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
                .metricName(CUSTOM_METRIC_NAME)
                .displayName(DISPLAY_NAME)
                .metricType(METRIC_TYPE)
                .metricArn(CUSTOM_METRIC_ARN)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ProxyThrowsAlreadyExists_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .displayName(DISPLAY_NAME)
                .metricType(METRIC_TYPE)
                .tags(MODEL_TAGS)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("TestCustomMetricIdentifier")
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
                .displayName(DISPLAY_NAME)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("MyResourceName")
                .clientRequestToken("MyToken")
                .desiredResourceTags(DESIRED_TAGS)
                .stackId("arn:aws:cloudformation:us-east-1:123456789012:stack/my-stack-name/" +
                        "084c0bd1-082b-11eb-afdc-0a2fadfa68a5")
                .build();

        CreateCustomMetricResponse createApiResponse = CreateCustomMetricResponse.builder()
                .metricArn(CUSTOM_METRIC_ARN)
                .metricName(CUSTOM_METRIC_NAME)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(createApiResponse);

        handler.handleRequest(proxy, request, null, logger);

        ArgumentCaptor<CreateCustomMetricRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateCustomMetricRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        CreateCustomMetricRequest submittedRequest = requestCaptor.getValue();
        // Can't easily check the randomly generated name. Just making sure it contains part of
        // the logical identifier and the stack name, and some more random characters
        assertThat(submittedRequest.metricName()).contains("my-stack");
        assertThat(submittedRequest.metricName()).contains("MyRes");
        assertThat(submittedRequest.metricName().length() > 20).isTrue();
    }

    // TODO: test system tags when the src code is ready
}
