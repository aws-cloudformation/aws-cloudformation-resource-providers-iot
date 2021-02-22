package com.amazonaws.iot.accountauditconfiguration;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeAccountAuditConfigurationResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;

public class TestConstants {
    static final String ACCOUNT_ID = "123456789012";
    static final String ROLE_ARN = "testRoleArn";
    static final String TARGET_ARN = "testTargetArn";

    static final AuditCheckConfiguration ENABLED_CFN =
            AuditCheckConfiguration.builder().enabled(true).build();
    static final AuditCheckConfiguration DISABLED_CFN =
            AuditCheckConfiguration.builder().enabled(false).build();
    static final AuditCheckConfigurations AUDIT_CHECK_CONFIGURATIONS_V1_CFN = AuditCheckConfigurations.builder()
            .loggingDisabledCheck(AuditCheckConfiguration.builder().enabled(true).build())
            .caCertificateExpiringCheck(AuditCheckConfiguration.builder().enabled(true).build())
            .build();
    static final AuditCheckConfigurations AUDIT_CHECK_CONFIGURATIONS_V2_CFN = AuditCheckConfigurations.builder()
            .loggingDisabledCheck(AuditCheckConfiguration.builder().enabled(true).build())
            .deviceCertificateExpiringCheck(AuditCheckConfiguration.builder().enabled(true).build())
            .caCertificateExpiringCheck(AuditCheckConfiguration.builder().enabled(false).build())
            .build();
    static final AuditNotificationTargetConfigurations AUDIT_NOTIFICATION_TARGET_CFN =
            AuditNotificationTargetConfigurations.builder()
                    .sns(AuditNotificationTarget.builder().enabled(true)
                            .targetArn(TARGET_ARN).roleArn(ROLE_ARN).build())
                    .build();
    static final AuditNotificationTargetConfigurations AUDIT_NOTIFICATION_TARGET_V2_CFN =
            AuditNotificationTargetConfigurations.builder().sns(AuditNotificationTarget.builder().enabled(true).targetArn(TARGET_ARN + "_v2").roleArn(ROLE_ARN).build()).build();

    static final software.amazon.awssdk.services.iot.model.AuditCheckConfiguration ENABLED_IOT =
            software.amazon.awssdk.services.iot.model.AuditCheckConfiguration.builder().enabled(true).build();
    static final software.amazon.awssdk.services.iot.model.AuditCheckConfiguration DISABLED_IOT =
            software.amazon.awssdk.services.iot.model.AuditCheckConfiguration.builder().enabled(false).build();
    static final Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
            AUDIT_NOTIFICATION_TARGET_IOT = ImmutableMap.of(
            "SNS", getNotificationBuilderIot().enabled(true)
                    .targetArn(TARGET_ARN).roleArn(ROLE_ARN).build());
    static final Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
            AUDIT_NOTIFICATION_TARGET_IOT_NON_EXISTENT_KEY = ImmutableMap.of(
            "SNS", getNotificationBuilderIot().enabled(true)
                    .targetArn(TARGET_ARN).roleArn(ROLE_ARN).build(),
            "NON_EXISTENT_KEY", getNotificationBuilderIot().enabled(true)
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
    static final AuditCheckConfigurations DESCRIBE_RESPONSE_V1_STATE_CFN = AuditCheckConfigurations.builder()
            .loggingDisabledCheck(ENABLED_CFN)
            .caCertificateExpiringCheck(ENABLED_CFN)
            .conflictingClientIdsCheck(DISABLED_CFN)
            .caCertificateKeyQualityCheck(DISABLED_CFN)
            .deviceCertificateExpiringCheck(DISABLED_CFN)
            .build();

    static final ImmutableMap<String,software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> ALL_CHECKS_MAP_IOT =
            ImmutableMap.<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration>builder()
                    .put("LOGGING_DISABLED_CHECK", ENABLED_IOT)
                    .put("CA_CERTIFICATE_EXPIRING_CHECK", ENABLED_IOT)
                    .put("CA_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_IOT)
                    .put("REVOKED_CA_CERTIFICATE_STILL_ACTIVE_CHECK", DISABLED_IOT)
                    .put("DEVICE_CERTIFICATE_EXPIRING_CHECK", DISABLED_IOT)
                    .put("DEVICE_CERTIFICATE_SHARED_CHECK", DISABLED_IOT)
                    .put("DEVICE_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_IOT)
                    .put("UNAUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK", DISABLED_IOT)
                    .put("AUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK", DISABLED_IOT)
                    .put("IOT_POLICY_OVERLY_PERMISSIVE_CHECK", DISABLED_IOT)
                    .put("IOT_ROLE_ALIAS_OVERLY_PERMISSIVE_CHECK", DISABLED_IOT)
                    .put("IOT_ROLE_ALIAS_ALLOWS_ACCESS_TO_UNUSED_SERVICES_CHECK", DISABLED_IOT)
                    .put("CONFLICTING_CLIENT_IDS_CHECK", DISABLED_IOT)
                    .put("REVOKED_DEVICE_CERTIFICATE_STILL_ACTIVE_CHECK", DISABLED_IOT)
                    .build();

    static final DescribeAccountAuditConfigurationResponse DESCRIBE_RESPONSE_ALL_CHECKS_PRESENT =
            DescribeAccountAuditConfigurationResponse.builder()
                    .auditCheckConfigurations(ALL_CHECKS_MAP_IOT)
                    .auditNotificationTargetConfigurationsWithStrings(AUDIT_NOTIFICATION_TARGET_IOT)
                    .roleArn(ROLE_ARN)
                    .build();

    static final AuditCheckConfigurations DESCRIBE_RESPONSE_ALL_CHECKS_PRESENT_CFN = AuditCheckConfigurations.builder()
            .loggingDisabledCheck(ENABLED_CFN)
            .caCertificateExpiringCheck(ENABLED_CFN)
            .conflictingClientIdsCheck(DISABLED_CFN)
            .caCertificateKeyQualityCheck(DISABLED_CFN)
            .deviceCertificateExpiringCheck(DISABLED_CFN)
            .authenticatedCognitoRoleOverlyPermissiveCheck(DISABLED_CFN)
            .deviceCertificateKeyQualityCheck(DISABLED_CFN)
            .deviceCertificateSharedCheck(DISABLED_CFN)
            .iotPolicyOverlyPermissiveCheck(DISABLED_CFN)
            .iotRoleAliasAllowsAccessToUnusedServicesCheck(DISABLED_CFN)
            .iotRoleAliasOverlyPermissiveCheck(DISABLED_CFN)
            .revokedCaCertificateStillActiveCheck(DISABLED_CFN)
            .revokedDeviceCertificateStillActiveCheck(DISABLED_CFN)
            .unauthenticatedCognitoRoleOverlyPermissiveCheck(DISABLED_CFN)
            .build();


    static final DescribeAccountAuditConfigurationResponse DESCRIBE_RESPONSE_V1_STATE_WITH_NON_EXISTENT_KEY =
            DescribeAccountAuditConfigurationResponse.builder()
                    .auditCheckConfigurations(ImmutableMap.of(
                            "LOGGING_DISABLED_CHECK", ENABLED_IOT,
                            "CA_CERTIFICATE_EXPIRING_CHECK", ENABLED_IOT,
                            "CONFLICTING_CLIENT_IDS_CHECK", DISABLED_IOT,
                            "CA_CERTIFICATE_KEY_QUALITY_CHECK", DISABLED_IOT,
                            "NON_EXISTENT_KEY", DISABLED_IOT))
                    .auditNotificationTargetConfigurationsWithStrings(AUDIT_NOTIFICATION_TARGET_IOT)
                    .roleArn(ROLE_ARN)
                    .build();
    static final AuditCheckConfigurations DESCRIBE_RESPONSE_V1_STATE_NON_EXISTENT_KEY_INGNORED_CFN =
            AuditCheckConfigurations.builder()
                    .loggingDisabledCheck(ENABLED_CFN)
                    .caCertificateExpiringCheck(ENABLED_CFN)
                    .conflictingClientIdsCheck(DISABLED_CFN)
                    .caCertificateKeyQualityCheck(DISABLED_CFN)
                    .build();
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
