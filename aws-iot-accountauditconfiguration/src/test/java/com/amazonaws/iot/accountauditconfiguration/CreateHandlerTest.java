package com.amazonaws.iot.accountauditconfiguration;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateAccountAuditConfigurationRequest;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ACCOUNT_ID;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_CHECK_CONFIGURATIONS_V1_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_REQUEST;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_ZERO_STATE;
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

    // CreateHandler throws RAE as soon as it sees a RoleArn already present, no matter whether it's the same or not.
    @Test
    public void handleRequest_DescribeShowsSameRoleArn_ExpectRAE() {

        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .auditCheckConfigurations(AUDIT_CHECK_CONFIGURATIONS_V1_CFN)
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN)
                .roleArn(ROLE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(eq(DESCRIBE_REQUEST), any()))
                .thenReturn(DESCRIBE_RESPONSE_V1_STATE);

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, cfnRequest, null, logger))
                .isInstanceOf(CfnAlreadyExistsException.class);
    }

    // CreateHandler throws RAE as soon as it sees a RoleArn already present, no matter whether it's the same or not.
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

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, cfnRequest, null, logger))
                .isInstanceOf(CfnAlreadyExistsException.class);
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

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
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

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }
}
