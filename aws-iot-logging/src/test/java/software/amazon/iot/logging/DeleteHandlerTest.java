package software.amazon.iot.logging;


import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.NotConfiguredException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends HandlerTestBase {

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        handler = new DeleteHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .defaultLogLevel(DEFAULT_LOG_LEVEL)
                .roleArn(ROLE_ARN)
                .build();

        ResourceHandlerRequest<ResourceModel> request = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(eq(GET_REQUEST), any()))
                .thenReturn(GET_RESPONSE_WITH_ALL_LOGS_ENABLED);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        ArgumentCaptor<IotRequest> iotRequestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(iotRequestCaptor.capture(), any());
        assertThat(iotRequestCaptor.getAllValues().get(1)).isEqualTo(SET_REQUEST_DISABLE_LOGS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AllLogsDisabled() {
        ResourceModel model = ResourceModel.builder()
                .accountId(ACCOUNT_ID)
                .defaultLogLevel(DEFAULT_LOG_LEVEL)
                .roleArn(ROLE_ARN)
                .build();

        ResourceHandlerRequest<ResourceModel> request = createCfnRequest(model);

        when(proxy.injectCredentialsAndInvokeV2(eq(GET_REQUEST), any()))
                .thenReturn(GET_RESPONSE_WITH_ALL_LOGS_DISABLED);

        ProgressEvent<ResourceModel, CallbackContext> actualResult =
                handler.handleRequest(proxy, request, null, logger);
        ProgressEvent<ResourceModel, CallbackContext> expectedResult = ProgressEvent.failed(
                model, null,
                HandlerErrorCode.NotFound,
                "The loggingOptions for your account doesn't exist.");
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    @Test
    public void handleRequest_InvalidRequestException() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(createDefaultModel());

        doThrow(InvalidRequestException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_UnauthorizedException() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(createDefaultModel());

        doThrow(UnauthorizedException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void handleRequest_InternalFailureException() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(createDefaultModel());

        doThrow(InternalFailureException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void handleRequest_ThrottlingException() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(createDefaultModel());

        doThrow(ThrottlingException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_ServiceUnavailableException() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(createDefaultModel());

        doThrow(ServiceUnavailableException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.GeneralServiceException);
    }

    @Test
    public void handleRequest_NotConfiguredException() {

        ResourceHandlerRequest<ResourceModel> cfnRequest = createCfnRequest(createDefaultModel());

        doThrow(NotConfiguredException.class)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(progressEvent.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }
}
