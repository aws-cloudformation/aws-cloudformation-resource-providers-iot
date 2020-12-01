package com.amazonaws.iot.mitigationaction;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.IndexNotReadyException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_ALL;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS_PARAMS_2;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_ALL;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TranslatorTest {

    @Test
    public void translateIotExceptionToCfn_LimitExceeded_Translated() {

        BaseHandlerException result = Translator.translateIotExceptionToCfn(LimitExceededException.builder().build());
        assertThat(result).isInstanceOf(CfnServiceLimitExceededException.class);
    }

    @Test
    public void translateIotExceptionToCfn_UnexpectedException_Rethrown() {

        IndexNotReadyException unexpectedException = IndexNotReadyException.builder().build();
        assertThatThrownBy(() -> Translator.translateIotExceptionToCfn(unexpectedException))
                .isInstanceOf(IndexNotReadyException.class);
    }

    @Test
    void translateTagsToSdk_InputNull_ReturnsEmpty() {
        assertThat(Translator.translateTagsToSdk(null)).isEmpty();
    }

    @Test
    void translateTagsToCfn_InputNull_ReturnsEmpty() {
        assertThat(Translator.translateTagsToCfn(null)).isEmpty();
    }

    @Test
    void translateActionParamsToSdk_ReplaceDefaultPolicyVersionParams() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)).isEqualTo(SDK_ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY);
    }

    @Test
    void translateActionParamsToSdk_AddThingsToThingGroupParams() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP_PARAMS)).isEqualTo(SDK_ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP);
    }

    @Test
    void translateActionParamsToSdk_EnableIoTLoggingParams() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS)).isEqualTo(SDK_ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS);
    }

    @Test
    void translateActionParamsToSdk_PublishFindingToSnsParams() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS_PARAMS_2)).isEqualTo(SDK_ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS);
    }

    @Test
    void translateActionParamsToSdk_UpdateCACertificateParams() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS)).isEqualTo(SDK_ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS);
    }

    @Test
    void translateActionParamsToSdk_UpdateDeviceCertificateParams() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS)).isEqualTo(SDK_ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS);
    }

    @Test
    void translateActionParamsToSdk_All() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS_WITH_ALL)).isEqualTo(SDK_ACTION_PARAMS_WITH_ALL);
    }

    @Test
    void translateActionParamsToCfnTest_ReplaceDefaultPolicyVersionParams() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY)).isEqualTo(ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS);
    }

    @Test
    void translateActionParamsToCfnTest_AddThingsToThingGroupParams() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP)).isEqualTo(ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP_PARAMS);
    }

    @Test
    void translateActionParamsToCfnTest_EnableIoTLoggingParams() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS)).isEqualTo(ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS);
    }

    @Test
    void translateActionParamsToCfnTest_PublishFindingToSnsParams() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS)).isEqualTo(ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS_PARAMS_2);
    }

    @Test
    void translateActionParamsToCfnTest_UpdateCACertificateParams() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS)).isEqualTo(ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS);
    }

    @Test
    void translateActionParamsToCfnTest_UpdateDeviceCertificateParams() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS)).isEqualTo(ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS);
    }

}
