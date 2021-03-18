package com.amazonaws.iot.scheduledaudit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.IndexNotReadyException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class TranslatorTest {

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void translateIotExceptionToCfn_LimitExceededErrorCode() {

        HandlerErrorCode result =
                Translator.translateExceptionToErrorCode(LimitExceededException.builder().build(), logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.ServiceLimitExceeded);
    }

    @Test
    public void translateIotExceptionToCfn_AccessDeniedErrorCode() {

        HandlerErrorCode result =
                Translator.translateExceptionToErrorCode(IotException.builder().statusCode(403)
                        .message("User not authorised to perform on resource with an explicit deny " +
                                "(Service: Iot, Status Code: 403, Request ID: dummy, " +
                                "Extended Request ID: null), stack trace")
                        .build(), logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void translateIotExceptionToCfn_UnexpectedErrorCode() {

        IndexNotReadyException unexpectedException = IndexNotReadyException.builder().build();
        HandlerErrorCode result = Translator.translateExceptionToErrorCode(unexpectedException, logger);
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.InternalFailure);
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
