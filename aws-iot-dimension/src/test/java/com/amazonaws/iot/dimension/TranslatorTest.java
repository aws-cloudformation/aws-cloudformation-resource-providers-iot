package com.amazonaws.iot.dimension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.IndexNotReadyException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

public class TranslatorTest {

    @Test
    public void translateIotExceptionToCfn_LimitExceeded_Translated() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(
                LimitExceededException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
    }

    @Test
    public void translateIotExceptionToCfn_UnexpectedException_Rethrown() {
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(
                IndexNotReadyException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    void translateTagsToSdk_InputNull_ReturnsEmpty() {
        assertThat(Translator.translateTagsToSdk(null)).isEmpty();
    }

    @Test
    void translateTagsToCfn_InputNull_ReturnsEmpty() {
        assertThat(Translator.translateTagsToCfn(null)).isEmpty();
    }
}
