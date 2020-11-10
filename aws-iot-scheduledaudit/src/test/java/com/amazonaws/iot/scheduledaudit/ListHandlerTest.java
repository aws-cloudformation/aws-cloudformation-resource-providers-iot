package com.amazonaws.iot.scheduledaudit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.ListScheduledAuditsRequest;
import software.amazon.awssdk.services.iot.model.ListScheduledAuditsResponse;
import software.amazon.awssdk.services.iot.model.ScheduledAuditMetadata;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

import static com.amazonaws.iot.scheduledaudit.TestConstants.DAY_OF_WEEK;
import static com.amazonaws.iot.scheduledaudit.TestConstants.DAY_OF_WEEK_2;
import static com.amazonaws.iot.scheduledaudit.TestConstants.FREQUENCY;
import static com.amazonaws.iot.scheduledaudit.TestConstants.FREQUENCY_2;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_ARN;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_ARN_2;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_NAME;
import static com.amazonaws.iot.scheduledaudit.TestConstants.SCHEDULED_AUDIT_NAME_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        ListScheduledAuditsRequest expectedRequest = ListScheduledAuditsRequest.builder()
                .nextToken(request.getNextToken())
                .build();
        ScheduledAuditMetadata ScheduledAuditMetadata1 = ScheduledAuditMetadata.builder()
                .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
                .frequency(FREQUENCY)
                .dayOfWeek(DAY_OF_WEEK)
                .build();
        ScheduledAuditMetadata ScheduledAuditMetadata2 = ScheduledAuditMetadata.builder()
                .scheduledAuditName(SCHEDULED_AUDIT_NAME_2)
                .scheduledAuditArn(SCHEDULED_AUDIT_ARN_2)
                .frequency(FREQUENCY_2)
                .dayOfWeek(DAY_OF_WEEK_2)
                .build();

        ListScheduledAuditsResponse listResponse = ListScheduledAuditsResponse.builder()
                .scheduledAudits(ScheduledAuditMetadata1, ScheduledAuditMetadata2)
                .nextToken("nextToken2")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest), any()))
                .thenReturn(listResponse);

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        assertThat(response.getNextToken()).isEqualTo("nextToken2");
        List<ResourceModel> expectedModels = Arrays.asList(
                ResourceModel.builder()
                        .scheduledAuditName(SCHEDULED_AUDIT_NAME)
                        .frequency(FREQUENCY)
                        .dayOfWeek(DAY_OF_WEEK)
                        .scheduledAuditArn(SCHEDULED_AUDIT_ARN)
                        .build(),
                ResourceModel.builder()
                        .scheduledAuditName(SCHEDULED_AUDIT_NAME_2)
                        .frequency(FREQUENCY_2)
                        .dayOfWeek(DAY_OF_WEEK_2)
                        .scheduledAuditArn(SCHEDULED_AUDIT_ARN_2)
                        .build());
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
    }

    @Test
    public void handleRequest_ThrowUnauthorized_VerifyTranslation() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(UnauthorizedException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnAccessDeniedException.class);
    }
}
