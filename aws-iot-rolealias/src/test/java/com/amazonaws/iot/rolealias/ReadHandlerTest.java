package com.amazonaws.iot.rolealias;

import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends RoleAliasTestBase{
    private ReadHandler handler = new ReadHandler();

    @Test
    public void handleRequest_SimpleSuccess() {

        final ResourceModel model = defaultModelBuilder().build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeRoleAlias(any(DescribeRoleAliasRequest.class))).thenReturn(TEST_DESCRIBE_ROLE_ALIAS_RESPONSE);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy,
                request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(iotClient).describeRoleAlias(any(DescribeRoleAliasRequest.class));
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .describeRoleAlias(any(DescribeRoleAliasRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .describeRoleAlias(any(DescribeRoleAliasRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ResourceNotFoundException.builder().build())
                .when(iotClient)
                .describeRoleAlias(any(DescribeRoleAliasRequest.class));

        Assertions.assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .describeRoleAlias(any(DescribeRoleAliasRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
