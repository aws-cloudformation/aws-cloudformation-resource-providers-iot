package com.amazonaws.iot.mitigationaction;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.SDK_ACTION_PARAMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.IndexNotReadyException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;

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
    void translateActionParamsToSdkTest() {
        assertThat(Translator.translateActionParamsToSdk(ACTION_PARAMS)).isEqualTo(SDK_ACTION_PARAMS);
    }

    @Test
    void translateActionParamsToCfnTest() {
        assertThat(Translator.translateActionParamsToCfn(SDK_ACTION_PARAMS)).isEqualTo(ACTION_PARAMS);
    }

}