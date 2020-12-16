package com.amazonaws.iot.scheduledaudit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeScheduledAuditRequest;
import software.amazon.awssdk.services.iot.model.DescribeScheduledAuditResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static com.amazonaws.iot.scheduledaudit.TestConstants.DAY_OF_WEEK;
import static com.amazonaws.iot.scheduledaudit.TestConstants.FREQUENCY;
import static com.amazonaws.iot.scheduledaudit.TestConstants.MODEL_TAGS;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_ARN;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_NAME;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SDK_MODEL_TAG;
import static com.amazonaws.iot.scheduledaudit.TestConstants.TARGET_CHECK_NAMES;
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
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DescribeScheduledAuditRequest expectedDescribeRequest = DescribeScheduledAuditRequest.builder()
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .build();
        DescribeScheduledAuditResponse describeResponse = DescribeScheduledAuditResponse.builder()
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
                .frequency(FREQUENCY)
                .targetCheckNames(TARGET_CHECK_NAMES)
                .dayOfWeek(DAY_OF_WEEK)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        doReturn(Collections.singletonList(SDK_MODEL_TAG))
                .when(handler)
                .listTags(proxy, SCHEDULED_AUDIT_ARN, logger);

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
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
                .targetCheckNames(TARGET_CHECK_NAMES)
                .tags(MODEL_TAGS)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_ThrowThrottling_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
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
