package software.amazon.iot.resourcespecificlogging;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.NotConfiguredException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExceptionTranslatorTest {

    @Test
    public void translateExceptionToErrorCode_InvalidRequestException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                InvalidRequestException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void translateExceptionToErrorCode_UnauthorizedException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                UnauthorizedException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void translateIotExceptionToCfn_AccessDeniedErrorCode() {
        HandlerErrorCode result =
                ExceptionTranslator.translateExceptionToErrorCode(IotException.builder().statusCode(403)
                        .message("User not authorised to perform on resource with an explicit deny " +
                                "(Service: Iot, Status Code: 403, Request ID: dummy, " +
                                "Extended Request ID: null), stack trace")
                        .build(), mock(Logger.class));
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void translateExceptionToErrorCode_InternalFailureException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                InternalFailureException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void translateExceptionToErrorCode_ThrottlingException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                ThrottlingException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void translateExceptionToErrorCode_LimitExceededException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                LimitExceededException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
    }

    @Test
    public void translateExceptionToErrorCode_ServiceUnavailableException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                ServiceUnavailableException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void translateExceptionToErrorCode_NotConfiguredException_Translated() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                NotConfiguredException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void translateExceptionToErrorCode_UnexpectedRNFE_TranslatedToInternalError() {
        HandlerErrorCode result = ExceptionTranslator.translateExceptionToErrorCode(
                ResourceNotFoundException.builder().build(), mock(Logger.class));
        assertThat(result).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}