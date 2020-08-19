package com.amazonaws.iot.authorizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AuthorizerTestBase {
    private final DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.describeAuthorizer(any(DescribeAuthorizerRequest.class))).thenReturn(TEST_DESCRIBE_AUTHORIZER_RESPONSE)
                .thenThrow(ResourceNotFoundException.builder().build());
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER);

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
    public void handleRequest_ActiveAuthorizerSucceeds() {
        final ResourceModel model = defaultModelBuilder().authorizerName("ActiveAuthorizer").status("ACTIVE").build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();
        when(iotClient.describeAuthorizer(any(DescribeAuthorizerRequest.class))).thenReturn(TEST_DESCRIBE_AUTHORIZER_RESPONSE)
                .thenThrow(ResourceNotFoundException.builder().build());

        // Need to call delete twice, because first response is in-progress of update to make authorizer status INACTIVE
        handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

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
    public void handleRequest_ResourceNotFound() {
        doThrow(ResourceNotFoundException.builder().build())
                .when(iotClient)
                .deleteAuthorizer(any(DeleteAuthorizerRequest.class));

        Assertions.assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .deleteAuthorizer(any(DeleteAuthorizerRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .deleteAuthorizer(any(DeleteAuthorizerRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .deleteAuthorizer(any(DeleteAuthorizerRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }
}
