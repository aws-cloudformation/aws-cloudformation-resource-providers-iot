package com.amazonaws.iot.certificate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.iot.model.CertificateStateException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // needed to mock same method with different params
public class UpdateHandlerTest extends CertificateTestBase {
    private UpdateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new UpdateHandler();
    }

    @Test
    public void handleRequest_updatesStatus() {
        final ResourceModel prevModel = defaultModelBuilder()
                .status(CERT_STATUS_ACTIVE)
                .build();
        final ResourceModel newModel = defaultModelBuilder()
                .status(CERT_STATUS_INACTIVE)
                .build();

        final String newStatus = "INACTIVE";
        newModel.setStatus(newStatus);

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getStatus()).isEqualTo(newStatus);

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    private void handleRequest_failsCertificateStateException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                CertificateStateException.builder().build(),
                CfnGeneralServiceException.class);
    }

    @Test
    private void handleRequest_failsInternalFailureException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                InternalFailureException.builder().build(),
                CfnServiceInternalErrorException.class);
    }

    @Test
    private void handleRequest_failsInvalidRequestException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                InvalidRequestException.builder().build(),
                CfnInvalidRequestException.class);
    }

    @Test
    private void handleRequest_failsResourceNotFoundException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                ResourceNotFoundException.builder().build(),
                CfnNotFoundException.class);
    }

    @Test
    private void handleRequest_failsServiceUnavailableException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                ServiceUnavailableException.builder().build(),
                CfnGeneralServiceException.class);
    }

    @Test
    private void handleRequest_failsThrottlingException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                ThrottlingException.builder().build(),
                CfnThrottlingException.class);
    }

    @Test
    private void handleRequest_failsUnauthorizedException() {
        testExceptionThrown(
                proxy,
                handler,
                logger,
                UnauthorizedException.builder().build(),
                CfnAccessDeniedException.class);
    }


}
