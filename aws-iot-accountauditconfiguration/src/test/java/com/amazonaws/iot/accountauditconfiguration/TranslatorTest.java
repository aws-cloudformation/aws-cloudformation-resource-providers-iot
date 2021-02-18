package com.amazonaws.iot.accountauditconfiguration;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_NON_EXISTENT_KEY_INGNORED_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_WITH_NON_EXISTENT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

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
    void translateNotificationsFromIotToCfn_NullIn_EmptyOut() {
        assertThat(Translator.translateNotificationsFromIotToCfn(null)).isNull();
    }
}
