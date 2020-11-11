package com.amazonaws.iot.accountauditconfiguration;

import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_CFN;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.AUDIT_NOTIFICATION_TARGET_IOT;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE;
import static com.amazonaws.iot.accountauditconfiguration.TestConstants.DESCRIBE_RESPONSE_V1_STATE_CFN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

public class TranslatorTest {

    @Test
    public void translateIotExceptionToCfn_IRE_Translated() {
        BaseHandlerException result = Translator.translateIotExceptionToCfn(InvalidRequestException.builder().build());
        assertThat(result).isInstanceOf(CfnInvalidRequestException.class);
    }

    @Test
    public void translateIotExceptionToCfn_RNFE_Rethrown() {
        ResourceNotFoundException rnfe = ResourceNotFoundException.builder().build();
        assertThatThrownBy(() -> Translator.translateIotExceptionToCfn(rnfe))
                .isInstanceOf(ResourceNotFoundException.class);
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
        assertThat(Translator.translateNotificationsFromIotToCfn(null)).isEmpty();
    }
}
