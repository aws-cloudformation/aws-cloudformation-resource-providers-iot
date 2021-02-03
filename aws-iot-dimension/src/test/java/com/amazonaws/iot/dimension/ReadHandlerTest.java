package com.amazonaws.iot.dimension;

import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_ARN;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_NAME;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_TYPE;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_VALUE_CFN;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_VALUE_IOT;
import static com.amazonaws.iot.dimension.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.dimension.TestConstants.SDK_MODEL_TAG_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeDimensionRequest;
import software.amazon.awssdk.services.iot.model.DescribeDimensionResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
                .name(DIMENSION_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DescribeDimensionRequest expectedDescribeRequest = DescribeDimensionRequest.builder()
                .name(DIMENSION_NAME)
                .build();
        DescribeDimensionResponse describeResponse = DescribeDimensionResponse.builder()
                .name(DIMENSION_NAME)
                .type(DIMENSION_TYPE)
                .stringValues(DIMENSION_VALUE_IOT)
                .arn(DIMENSION_ARN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        doReturn(Collections.singletonList(SDK_MODEL_TAG_1))
                .when(handler)
                .listTags(proxy, DIMENSION_ARN, logger);

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
                .name(DIMENSION_NAME)
                .type(DIMENSION_TYPE)
                .stringValues(DIMENSION_VALUE_CFN)
                .arn(DIMENSION_ARN)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ThrowThrottling_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .name(DIMENSION_NAME)
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
