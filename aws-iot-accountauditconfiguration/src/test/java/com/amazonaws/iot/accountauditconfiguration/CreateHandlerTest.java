package com.amazonaws.iot.accountauditconfiguration;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ACCOUNT_ID;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_CHECK_CONFIGURATIONS_V1_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_REQUEST;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE_CHECKS;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DISABLED_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ENABLED_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ENABLED_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ROLE_ARN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.createCfnRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateAccountAuditConfigurationRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration>
            CHECK_CONFIGURATION_FROM_EXPECTED_UPDATE_REQUEST = ImmutableMap.of(
            "LOGGING_DISABLED_CHECK", ENABLED_IOT,
            "CA_CERTIFICATE_EXPIRING_CHECK", ENABLED_IOT);

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_AllPropertiesFilledOut_FirstTimeCreate_VerifyInteractions() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(DESCRIBE_RESPONSE_ZERO_STATE);

        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, cfnRequest, null, logger);

        UpdateAccountAuditConfigurationRequest expectedUpdateRequest = UpdateAccountAuditConfigurationRequest.builder()
                .auditCheckConfigurations(CHECK_CONFIGURATION_FROM_EXPECTED_UPDATE_REQUEST)
                .auditNotificationTargetConfigurationsWithStrings(AUDIT_NOTIFICATION_TARGET_IOT)
                .roleArn(ROLE_ARN)
                .build();
        ArgumentCaptor<IotRequest> iotRequestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(iotRequestCaptor.capture(), any());
        assertThat(iotRequestCaptor.getAllValues().get(1)).isEqualTo(expectedUpdateRequest);

        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackContext()).isNull();
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();
        assertThat(handlerResponse.getResourceModel()).isEqualTo(model);
    }

    @Test
    public void handleRequest_DescribeShowsIdenticalConfig_ExpectSuccess_NoUpdateCall() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE);

        ProgressEvent<ResourceModel, CallbackContext> handlerResponse =
                handler.handleRequest(proxy, cfnRequest, null, logger);

        ArgumentCaptor<IotRequest> iotRequestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(iotRequestCaptor.capture(), any());
        assertThat(iotRequestCaptor.getAllValues()).isEqualTo(
                Collections.singletonList(DescribeAccountAuditConfigurationRequest.builder().build()));

        assertThat(handlerResponse).isNotNull();
        assertThat(handlerResponse.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(handlerResponse.getCallbackContext()).isNull();
        assertThat(handlerResponse.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(handlerResponse.getResourceModels()).isNull();
        assertThat(handlerResponse.getMessage()).isNull();
        assertThat(handlerResponse.getErrorCode()).isNull();
        assertThat(handlerResponse.getResourceModel()).isEqualTo(model);
    }

    @Test
    public void handleRequest_DescribeShowsDifferentRoleArn_ExpectRAE() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        DescribeAccountAuditConfigurationResponse describeResponseWithDifferentRoleArn =
                DESCRIBE_RESPONSE_V1_STATE.toBuilder().roleArn("differentRoleArn").build();
        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(describeResponseWithDifferentRoleArn);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        ProgressEvent<ResourceModel, CallbackContext> expectedResult = ProgressEvent.failed(
                model, null,
                HandlerErrorCode.AlreadyExists,
                "A configuration with different properties already exists.");
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void areCheckConfigurationsEquivalent_DescribeHasMoreDisabledChecks_ExpectTrue() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> mapWithMoreChecksDisabled =
                new HashMap<>(CHECK_CONFIGURATION_FROM_EXPECTED_UPDATE_REQUEST);
        mapWithMoreChecksDisabled.put("CONFLICTING_CLIENT_IDS_CHECK", DISABLED_IOT);
        DescribeAccountAuditConfigurationResponse describeResponse =
                DESCRIBE_RESPONSE_V1_STATE.toBuilder()
                        .auditCheckConfigurations(mapWithMoreChecksDisabled)
                        .build();

        assertThat(handler.areCheckConfigurationsEquivalent(model, describeResponse)).isTrue();
    }

    @Test
    public void areCheckConfigurationsEquivalent_DescribeHasOneMoreEnabled_ExpectFalse() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> mapWithMoreChecksEnabled =
                new HashMap<>(DESCRIBE_RESPONSE_ZERO_STATE_CHECKS);
        mapWithMoreChecksEnabled.put("CONFLICTING_CLIENT_IDS_CHECK", ENABLED_IOT);
        DescribeAccountAuditConfigurationResponse describeResponse =
                DESCRIBE_RESPONSE_V1_STATE.toBuilder()
                        .auditCheckConfigurations(mapWithMoreChecksEnabled)
                        .build();

        assertThat(handler.areCheckConfigurationsEquivalent(model, describeResponse)).isFalse();
    }

    @Test
    public void areCheckConfigurationsEquivalent_ModelHasOneMoreEnabled_ExpectFalse() {

        Map<String, AuditCheckConfiguration> mapWithOneMoreCheckEnabled =
                new HashMap<>(AUDIT_CHECK_CONFIGURATIONS_V1_CFN);
        mapWithOneMoreCheckEnabled.put("CA_CERTIFICATE_KEY_QUALITY_CHECK", ENABLED_CFN);
        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(mapWithOneMoreCheckEnabled)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();

        assertThat(handler.areCheckConfigurationsEquivalent(model, DESCRIBE_RESPONSE_V1_STATE)).isFalse();
    }

    @Test
    public void areNotificationTargetsEquivalent_DescribeHasNull_ExpectFalse() {
        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        assertThat(handler.areNotificationTargetsEquivalent(model, DESCRIBE_RESPONSE_ZERO_STATE)).isFalse();
    }

    @Test
    public void areNotificationTargetsEquivalent_ModelHasNull_ExpectFalse() {
        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(null)
                .roleArn(ROLE_ARN)
                .build();
        assertThat(handler.areNotificationTargetsEquivalent(model, DESCRIBE_RESPONSE_V1_STATE)).isFalse();
    }

    @Test
    public void areNotificationTargetsEquivalent_DifferentTargetArns_ExpectFalse() {
        Map<String, AuditNotificationTarget> mapWithDifferentTargetArn = ImmutableMap.of(
                "SNS", AuditNotificationTarget.builder().enabled(true)
                        .targetArn("differentTargetArn").roleArn(ROLE_ARN).build());
        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(mapWithDifferentTargetArn)
                .roleArn(ROLE_ARN)
                .build();
        assertThat(handler.areNotificationTargetsEquivalent(model, DESCRIBE_RESPONSE_V1_STATE)).isFalse();
    }

    @Test
    public void handleRequest_WrongAccountId_ExpectIRE() {

        ResourceModel model = ResourceModel.builder()
                .accountId("000111222333")
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        ProgressEvent<ResourceModel, CallbackContext> expectedResult = ProgressEvent.failed(
                model, null,
                HandlerErrorCode.InvalidRequest,
                "AccountId in the template (000111222333) doesn't match actual: 123456789012.");
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void handleRequest_ExceptionFromDescribe_Translated() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenThrow(UnauthorizedException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, cfnRequest, null, logger))
                .isInstanceOf(CfnAccessDeniedException.class);
    }

    @Test
    public void handleRequest_ExceptionFromCreate_Translated() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DESCRIBE_RESPONSE_ZERO_STATE)
                .thenThrow(InvalidRequestException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, cfnRequest, null, logger))
                .isInstanceOf(CfnInvalidRequestException.class);
    }
}
