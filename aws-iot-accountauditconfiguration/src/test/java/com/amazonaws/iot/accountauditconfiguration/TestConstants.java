package com.amazonaws.iot.accountauditconfiguration;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class TestConstants {

    static final String ACCOUNT_ID = "123456789012";
    static final String ROLE_ARN = "testRoleArn";
    static final String TARGET_ARN = "testTargetArn";

    static final AuditCheckConfiguration ENABLED_CFN =
            AuditCheckConfiguration.builder().enabled(true).build();
    static final AuditCheckConfiguration DISABLED_CFN =
            AuditCheckConfiguration.builder().enabled(false).build();
    static final Map<String, AuditCheckConfiguration> AUDIT_CHECK_CONFIGURATIONS_V1_CFN = ImmutableMap.of(
            "LOGGING_DISABLED_CHECK", ENABLED_CFN,
            "CA_CERTIFICATE_EXPIRING_CHECK", ENABLED_CFN);
    static final Map<String, AuditCheckConfiguration> AUDIT_CHECK_CONFIGURATIONS_V2_CFN = ImmutableMap.of(
            "LOGGING_DISABLED_CHECK", ENABLED_CFN,
            "DEVICE_CERTIFICATE_EXPIRING_CHECK", ENABLED_CFN,
            "CA_CERTIFICATE_EXPIRING_CHECK", DISABLED_CFN);
    static final Map<String, AuditNotificationTarget> AUDIT_NOTIFICATION_TARGET_CFN = ImmutableMap.of(
            "SNS", AuditNotificationTarget.builder().enabled(true)
                    .targetArn(TARGET_ARN).roleArn(ROLE_ARN).build());
    static final Map<String, AuditNotificationTarget> AUDIT_NOTIFICATION_TARGET_V2_CFN = ImmutableMap.of(
            "SNS", AuditNotificationTarget.builder().enabled(true)
                    .targetArn(TARGET_ARN + "_v2").roleArn(ROLE_ARN).build());

    static final software.amazon.awssdk.services.iot.model.AuditCheckConfiguration ENABLED_IOT =
            software.amazon.awssdk.services.iot.model.AuditCheckConfiguration.builder().enabled(true).build();
    static final software.amazon.awssdk.services.iot.model.AuditCheckConfiguration DISABLED_IOT =
            software.amazon.awssdk.services.iot.model.AuditCheckConfiguration.builder().enabled(false).build();
    static final Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
            AUDIT_NOTIFICATION_TARGET_IOT = ImmutableMap.of(
            "SNS", getNotificationBuilderIot().enabled(true)
                    .targetArn(TARGET_ARN).roleArn(ROLE_ARN).build());
    static final Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
            AUDIT_NOTIFICATION_TARGET_V2_IOT = ImmutableMap.of(
            "SNS", getNotificationBuilderIot().enabled(true)
                    .targetArn(TARGET_ARN + "_v2").roleArn(ROLE_ARN).build());

    static final DescribeAccountAuditConfigurationRequest DESCRIBE_REQUEST =
            DescribeAccountAuditConfigurationRequest.builder().build();

    static final Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration>
            DESCRIBE_RESPONSE_ZERO_STATE_CHECKS = ImmutableMap.of(
            "CONFLICTING_CLIENT_IDS_CHECK", DISABLED_IOT,
            "CA_CERTIFICATE_EXPIRING_CHECK", DISABLED_IOT,
            "CA_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_IOT,
            "DEVICE_CERTIFICATE_EXPIRING_CHECK", DISABLED_IOT,
            "LOGGING_DISABLED_CHECK", DISABLED_IOT);
    static final DescribeAccountAuditConfigurationResponse DESCRIBE_RESPONSE_ZERO_STATE =
            DescribeAccountAuditConfigurationResponse.builder()
                    .auditCheckConfigurations(DESCRIBE_RESPONSE_ZERO_STATE_CHECKS)
                    .build();
    static final DescribeAccountAuditConfigurationResponse DESCRIBE_RESPONSE_V1_STATE =
            DescribeAccountAuditConfigurationResponse.builder()
                    .auditCheckConfigurations(ImmutableMap.of(
                            "LOGGING_DISABLED_CHECK", ENABLED_IOT,
                            "CA_CERTIFICATE_EXPIRING_CHECK", ENABLED_IOT,
                            "CONFLICTING_CLIENT_IDS_CHECK", DISABLED_IOT,
                            "CA_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_IOT,
                            "DEVICE_CERTIFICATE_EXPIRING_CHECK", DISABLED_IOT))
                    .auditNotificationTargetConfigurationsWithStrings(AUDIT_NOTIFICATION_TARGET_IOT)
                    .roleArn(ROLE_ARN)
                    .build();
    static final Map<String, AuditCheckConfiguration> DESCRIBE_RESPONSE_V1_STATE_CFN = ImmutableMap.of(
            "LOGGING_DISABLED_CHECK", ENABLED_CFN,
            "CA_CERTIFICATE_EXPIRING_CHECK", ENABLED_CFN,
            "CONFLICTING_CLIENT_IDS_CHECK", DISABLED_CFN,
            "CA_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_CFN,
            "DEVICE_CERTIFICATE_EXPIRING_CHECK", DISABLED_CFN);

    static software.amazon.awssdk.services.iot.model.AuditNotificationTarget.Builder
    getNotificationBuilderIot() {
        return software.amazon.awssdk.services.iot.model.AuditNotificationTarget.builder();
    }

    static ResourceHandlerRequest<ResourceModel> createCfnRequest(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("doesn't matter")
                .awsAccountId(ACCOUNT_ID)
                .build();
    }
}
