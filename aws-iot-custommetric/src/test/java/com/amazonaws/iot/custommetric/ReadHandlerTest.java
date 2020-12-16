package com.amazonaws.iot.custommetric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeCustomMetricRequest;
import software.amazon.awssdk.services.iot.model.DescribeCustomMetricResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_ARN;
import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_NAME;
import static com.amazonaws.iot.custommetric.TestConstants.DISPLAY_NAME;
import static com.amazonaws.iot.custommetric.TestConstants.METRIC_TYPE;
import static com.amazonaws.iot.custommetric.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.custommetric.TestConstants.SDK_MODEL_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private ReadHandler handler;

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceModel model = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DescribeCustomMetricRequest expectedDescribeRequest = DescribeCustomMetricRequest.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .build();
        DescribeCustomMetricResponse describeResponse = DescribeCustomMetricResponse.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .displayName(DISPLAY_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        doReturn(Collections.singletonList(SDK_MODEL_TAG))
                .when(handler)
                .listTags(proxy, CUSTOM_METRIC_ARN, logger);

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
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ThrowThrottling_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .metricArn(CUSTOM_METRIC_ARN)
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
