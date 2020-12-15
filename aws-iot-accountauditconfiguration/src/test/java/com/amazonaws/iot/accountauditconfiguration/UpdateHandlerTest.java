package com.amazonaws.iot.accountauditconfiguration;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ACCOUNT_ID;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_CHECK_CONFIGURATIONS_V2_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_V2_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_V2_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_REQUEST;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DISABLED_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DISABLED_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ENABLED_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ROLE_ARN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.createCfnRequest;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.getNotificationBuilderIot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UpdateAccountAuditConfigurationRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    private static final Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> CHECK_CONFIGURATION_FROM_EXPECTED_UPDATE_REQUEST =
            ImmutableMap.of(
                    // enable 1, disable 1
                    "LOGGING_DISABLED_CHECK", ENABLED_IOT,
                    "DEVICE_CERTIFICATE_EXPIRING_CHECK", ENABLED_IOT,
                    "CA_CERTIFICATE_EXPIRING_CHECK", DISABLED_IOT,
                    "CONFLICTING_CLIENT_IDS_CHECK", DISABLED_IOT,
                    "CA_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_IOT);
    private static final ResourceModel MODEL_V2 = ResourceModel.builder()
            .accountId(ACCOUNT_ID)
            .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V2_CFN)
            .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_V2_CFN)
            .roleArn(ROLE_ARN + "_v2")
            .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
    }

    @Test
    public void handleRequest_DescribeShowsDifferentConfig_UpdateOverwritesEverything() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_V2);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE);

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, cfnRequest, null, logger);

        UpdateAccountAuditConfigurationRequest expectedUpdateRequest = UpdateAccountAuditConfigurationRequest.builder()
                .auditCheckConfigurations(CHECK_CONFIGURATION_FROM_EXPECTED_UPDATE_REQUEST)
                .auditNotificationTargetConfigurationsWithStrings(AUDIT_NOTIFICATION_TARGET_V2_IOT)
                .roleArn(ROLE_ARN + "_v2")
                .build();
        ArgumentCaptor<IotRequest> iotRequestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(iotRequestCaptor.capture(), any());
        assertThat(iotRequestCaptor.getAllValues().get(1)).isEqualTo(expectedUpdateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(cfnRequest.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DescribeShowsZeroState_ExpectNFE() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_V2);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_ZERO_STATE);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        ProgressEvent<ResourceModel, CallbackContext> expectedResult = ProgressEvent.failed(
                MODEL_V2, null,
                HandlerErrorCode.NotFound,
                "The configuration for your account has not been set up or was deleted.");
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void buildCheckConfigurationsForUpdate_RequestHasNonExistentCheck_ExpectRetention() {

        Map<String, AuditCheckConfiguration> checks = new HashMap<>(AUDIT_CHECK_CONFIGURATIONS_V2_CFN);
        checks.put("NonExistentCheck", DISABLED_CFN);
        ResourceModel model = ResourceModel.builder()
                .auditCheckConfigurations(checks)
                .build();

        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> actualResult =
                handler.buildCheckConfigurationsForUpdate(model, DESCRIBE_RESPONSE_V1_STATE);

        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> expected =
                new HashMap<>(CHECK_CONFIGURATION_FROM_EXPECTED_UPDATE_REQUEST);
        expected.put("NonExistentCheck", DISABLED_IOT);

        assertThat(actualResult).isEqualTo(expected);
    }

    @Test
    public void buildNotificationTargetConfigurationsForUpdate_NoneInModel_DisabledInResult() {

        ResourceModel model = ResourceModel.builder()
                .auditNotificationTargetConfigurations(null)
                .build();

        Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> expectedResult =
                ImmutableMap.of("SNS", getNotificationBuilderIot().enabled(false).build());
        Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> actualResult =
                handler.buildNotificationTargetConfigurationsForUpdate(model);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void buildNotificationTargetConfigurationsForUpdate_NonExistentTarget_ExpectRetention() {

        ImmutableMap<String, AuditNotificationTarget> notifications = ImmutableMap.of(
                "NonExistent", AuditNotificationTarget.builder().enabled(false).build());
        ResourceModel model = ResourceModel.builder()
                .auditNotificationTargetConfigurations(notifications)
                .build();

        Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> expectedResult =
                ImmutableMap.of(
                        "SNS", getNotificationBuilderIot().enabled(false).build(),
                        "NonExistent", getNotificationBuilderIot().enabled(false).build());
        Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> actualResult =
                handler.buildNotificationTargetConfigurationsForUpdate(model);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void handleRequest_ExceptionFromDescribe_Translated() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_V2);
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenThrow(InternalFailureException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_ExceptionFromUpdate_Translated() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(MODEL_V2);
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE)
                .thenThrow(ThrottlingException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }
}
