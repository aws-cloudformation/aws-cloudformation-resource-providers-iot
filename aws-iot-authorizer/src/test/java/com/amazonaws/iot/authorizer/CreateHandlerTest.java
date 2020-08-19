package com.amazonaws.iot.authorizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AuthorizerTestBase {

    private final CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.describeAuthorizer(any(DescribeAuthorizerRequest.class)))
                .thenReturn(TEST_DESCRIBE_AUTHORIZER_RESPONSE);
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(TEST_REQUEST.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getAuthorizerName()).isEqualTo(AUTHORIZER_NAME);
        assertThat(response.getResourceModel().getAuthorizerFunctionArn()).isEqualTo(AUTHORIZER_FUNCTION_ARN);

        verify(iotClient).createAuthorizer(any(CreateAuthorizerRequest.class));
    }

    @Test
    public void handleRequest_GeneratesName() {
        final ResourceModel model = defaultModelBuilder().authorizerName(null).build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();
        final ArgumentCaptor<DescribeAuthorizerRequest> authorizerRequestCaptor = ArgumentCaptor.forClass(DescribeAuthorizerRequest.class);

        when(iotClient.describeAuthorizer(any(DescribeAuthorizerRequest.class)))
                .thenReturn(TEST_DESCRIBE_AUTHORIZER_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        verify(iotClient).describeAuthorizer(authorizerRequestCaptor.capture());
        assertThat(authorizerRequestCaptor.getValue().authorizerName()).isNotNull();
    }

    @Test
    public void handleRequest_ResourceConflictFails() {
        doThrow(ResourceAlreadyExistsException.builder().resourceId(AUTHORIZER_NAME).build())
                .when(iotClient)
                .createAuthorizer(any(CreateAuthorizerRequest.class));

        Assertions.assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .createAuthorizer(any(CreateAuthorizerRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_LimitExceededFails() {
        doThrow(LimitExceededException.builder().build())
                .when(iotClient)
                .createAuthorizer(any(CreateAuthorizerRequest.class));

        Assertions.assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .createAuthorizer(any(CreateAuthorizerRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .createAuthorizer(any(CreateAuthorizerRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }
}
