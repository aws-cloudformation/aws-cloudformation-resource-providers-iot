package com.amazonaws.iot.accountauditconfiguration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ACCOUNT_ID;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_CHECK_CONFIGURATIONS_V1_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_REQUEST;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ROLE_ARN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.TARGET_ARN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.createCfnRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest {
    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();
    }

    @Test
    public void handleRequest_DescribeHasFullConfig_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .accountId("doesn't matter")
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn("doesn't matter")
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
        assertThat(actualResult.getResourceModels()).isNull();
        assertThat(actualResult.getMessage()).isNull();
        assertThat(actualResult.getErrorCode()).isNull();

        AuditNotificationTargetConfigurations expectedNotificationConfigurations =
                AuditNotificationTargetConfigurations.builder()
                        .sns(AuditNotificationTarget.builder().targetArn(TARGET_ARN).enabled(true).roleArn(ROLE_ARN).build()).build();
        ResourceModel expectedResult = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(DESCRIBE_RESPONSE_V1_STATE_CFN)
                .auditNotificationTargetConfigurations(expectedNotificationConfigurations)
                .roleArn(ROLE_ARN)
                .build();
        assertThat(actualResult.getResourceModel()).isEqualTo(expectedResult);
    }

    @Test
    public void handleRequest_DescribeReturnsZeroState_ExpectNFE() {

        ResourceModel model = ResourceModel.builder()
                .accountId("doesn't matter")
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn("doesn't matter")
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(DESCRIBE_RESPONSE_ZERO_STATE);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        ProgressEvent<ResourceModel, CallbackContext> expectedResult = ProgressEvent.failed(
                model, null,
                HandlerErrorCode.NotFound,
                "The configuration for your account has not been set up or was deleted.");
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void handleRequest_ExceptionFromDescribe_Translated() {

        ResourceModel model = ResourceModel.builder()
                .accountId("doesn't matter")
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn("doesn't matter")
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenThrow(UnauthorizedException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
