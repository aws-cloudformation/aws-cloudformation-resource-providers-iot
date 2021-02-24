package com.amazonaws.iot.accountauditconfiguration;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Arrays;
import java.util.Map;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_IOT_NON_EXISTENT_KEY;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_NON_EXISTENT_KEY_INGNORED_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_WITH_NON_EXISTENT_KEY;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.ENABLED_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.getAuditCheckConfigurationsAllChecksDisabledCfn;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.getDescribeAccountAuditConfigurationResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class TranslatorTest {
    @Test
    public void translateExceptionToErrorCode_IRE_Translated() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(
                InvalidRequestException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void translateExceptionToErrorCode_UnexpectedRNFE_TranslatedToInternalError() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(
                ResourceNotFoundException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    void translateChecksFromCfnToIot_NonNull_VerifyTranslation() {
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> expectedResult =
                DESCRIBE_RESPONSE_V1_STATE.auditCheckConfigurations();
        ResourceModel input = ResourceModel.builder()
                .auditCheckConfigurations(DESCRIBE_RESPONSE_V1_STATE_CFN).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);
    }

    @Test
    void translateChecksFromIotToCfn_NonNull_VerifyTranslation() {
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> input =
                DESCRIBE_RESPONSE_V1_STATE.auditCheckConfigurations();
        assertThat(Translator.translateChecksFromIotToCfn(input)).isEqualTo(DESCRIBE_RESPONSE_V1_STATE_CFN);
    }

    @Test
    void translateChecksFromIotToCfn_NonExistentKeyFromIot_VerifyIgnoredInTranslation() {
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> input =
                DESCRIBE_RESPONSE_V1_STATE_WITH_NON_EXISTENT_KEY.auditCheckConfigurations();
        assertThat(Translator.translateChecksFromIotToCfn(input)).isEqualTo(DESCRIBE_RESPONSE_V1_STATE_NON_EXISTENT_KEY_INGNORED_CFN);
    }

    @Test
    void translateNotificationsFromCfnToIot_NonEmpty() {
        ResourceModel input = ResourceModel.builder()
                .auditNotificationTargetConfigurations(AUDIT_NOTIFICATION_TARGET_CFN).build();
        assertThat(Translator.translateNotificationsFromCfnToIot(input)).isEqualTo(AUDIT_NOTIFICATION_TARGET_IOT);
    }

    @Test
    void translateNotificationsFromCfnToIot_NullIn_EmptyOut() {
        ResourceModel input = ResourceModel.builder()
                .auditNotificationTargetConfigurations(null).build();
        assertThat(Translator.translateNotificationsFromCfnToIot(input)).isEmpty();
    }

    @Test
    void translateNotificationsFromIotToCfn_NonEmpty() {
        assertThat(Translator.translateNotificationsFromIotToCfn(AUDIT_NOTIFICATION_TARGET_IOT))
                .isEqualTo(AUDIT_NOTIFICATION_TARGET_CFN);
    }

    @Test
    void translateNotificationsFromIotToCfn_NullIn_NullOut() {
        assertThat(Translator.translateNotificationsFromIotToCfn(null)).isNull();
    }

    @Test
    void translateNotificationsFromIotToCfn_NonExistentKeyFromIot_VerifyIgnoredInTranslation() {
        assertThat(Translator.translateNotificationsFromIotToCfn(AUDIT_NOTIFICATION_TARGET_IOT_NON_EXISTENT_KEY))
                .isEqualTo(AUDIT_NOTIFICATION_TARGET_CFN);
    }

    @Test
    void translateChecksFromCfnToIot_NullIn_EmptyOut() {
        ResourceModel input = ResourceModel.builder().build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEmpty();
    }

    @Test
    void translateChecksFromCfnToIot_AllChecksArePresent_VerifyTranslation() {
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("LOGGING_DISABLED_CHECK", "CA_CERTIFICATE_EXPIRING_CHECK"))
                        .auditCheckConfigurations();
        AuditCheckConfigurations auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setLoggingDisabledCheck(ENABLED_CFN);
        auditCheckConfigurations.setCaCertificateExpiringCheck(ENABLED_CFN);
        ResourceModel input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);
    }

    @Test
    void translateChecksFromIotToCfn_AllChecksArePresent() {
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> input =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("LOGGING_DISABLED_CHECK", "CA_CERTIFICATE_EXPIRING_CHECK"))
                        .auditCheckConfigurations();
        AuditCheckConfigurations auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setLoggingDisabledCheck(ENABLED_CFN);
        auditCheckConfigurations.setCaCertificateExpiringCheck(ENABLED_CFN);
        assertThat(Translator.translateChecksFromIotToCfn(input)).isEqualTo(auditCheckConfigurations);
    }

    @Test
    void translateChecksFromCfnToIot_OneCheckEnabledRestDisabled_VerifyTranslation() {
        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("LOGGING_DISABLED_CHECK")).auditCheckConfigurations();
        AuditCheckConfigurations auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setLoggingDisabledCheck(ENABLED_CFN);
        ResourceModel input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("CA_CERTIFICATE_EXPIRING_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setCaCertificateExpiringCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("CA_CERTIFICATE_KEY_QUALITY_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setCaCertificateKeyQualityCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("REVOKED_CA_CERTIFICATE_STILL_ACTIVE_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setRevokedCaCertificateStillActiveCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("DEVICE_CERTIFICATE_EXPIRING_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setDeviceCertificateExpiringCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("DEVICE_CERTIFICATE_SHARED_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setDeviceCertificateSharedCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("DEVICE_CERTIFICATE_KEY_QUALITY_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setDeviceCertificateKeyQualityCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("UNAUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setUnauthenticatedCognitoRoleOverlyPermissiveCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("AUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setAuthenticatedCognitoRoleOverlyPermissiveCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("IOT_POLICY_OVERLY_PERMISSIVE_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setIotPolicyOverlyPermissiveCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("IOT_ROLE_ALIAS_OVERLY_PERMISSIVE_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setIotRoleAliasOverlyPermissiveCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("IOT_ROLE_ALIAS_ALLOWS_ACCESS_TO_UNUSED_SERVICES_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setIotRoleAliasAllowsAccessToUnusedServicesCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("CONFLICTING_CLIENT_IDS_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setConflictingClientIdsCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);

        expectedResult =
                getDescribeAccountAuditConfigurationResponse(Arrays.asList("REVOKED_DEVICE_CERTIFICATE_STILL_ACTIVE_CHECK")).auditCheckConfigurations();
        auditCheckConfigurations = getAuditCheckConfigurationsAllChecksDisabledCfn();
        auditCheckConfigurations.setRevokedDeviceCertificateStillActiveCheck(ENABLED_CFN);
        input = ResourceModel.builder()
                .auditCheckConfigurations(auditCheckConfigurations).build();
        assertThat(Translator.translateChecksFromCfnToIot(input)).isEqualTo(expectedResult);
    }

}
