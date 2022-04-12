package com.amazonaws.iot.rolealias;

import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.RoleAliasDescription;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UpdateRoleAliasRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends RoleAliasTestBase{
    final UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_updatesNewRoleArn() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setRoleArn(UPDATE_ROLE_ARN);

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        RoleAliasDescription description = RoleAliasDescription.builder()
                .credentialDurationSeconds(CREDENTIAL_DURATION_SECONDS)
                .roleAlias(ROLE_ALIAS)
                .roleAliasArn(ROLE_ALIAS_ARN)
                .roleArn(UPDATE_ROLE_ARN)
                .owner("123456789012")
                .creationDate(Instant.now())
                .lastModifiedDate(Instant.now())
                .build();
        when(iotClient.describeRoleAlias(any(DescribeRoleAliasRequest.class)))
                .thenReturn(DescribeRoleAliasResponse.builder()
                        .roleAliasDescription(description)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getRoleAlias()).isEqualTo(ROLE_ALIAS);
        assertThat(response.getResourceModel().getRoleAliasArn()).isEqualTo(ROLE_ALIAS_ARN);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(UPDATE_ROLE_ARN);
    }

    @Test
    public void handleRequest_updatesCredentialDurationTime() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setCredentialDurationSeconds(1000);

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();


        RoleAliasDescription description = RoleAliasDescription.builder()
                .credentialDurationSeconds(1000)
                .roleAlias(ROLE_ALIAS)
                .roleAliasArn(ROLE_ALIAS_ARN)
                .roleArn(ROLE_ARN)
                .owner("123456789012")
                .creationDate(Instant.now())
                .lastModifiedDate(Instant.now())
                .build();
        when(iotClient.describeRoleAlias(any(DescribeRoleAliasRequest.class)))
                .thenReturn(DescribeRoleAliasResponse.builder()
                        .roleAliasDescription(description)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getRoleAlias()).isEqualTo(ROLE_ALIAS);
        assertThat(response.getResourceModel().getRoleAliasArn()).isEqualTo(ROLE_ALIAS_ARN);
        assertThat(response.getResourceModel().getRoleArn()).isEqualTo(ROLE_ARN);
        assertThat(response.getResourceModel().getCredentialDurationSeconds()).isEqualTo(1000);
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        newModel.setRoleArn(UPDATE_ROLE_ARN);

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .updateRoleAlias(any(UpdateRoleAliasRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));

    }

    @Test
    public void handleRequest_CfnThrottlingException() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        newModel.setRoleArn(UPDATE_ROLE_ARN);

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .updateRoleAlias(any(UpdateRoleAliasRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnInvalidRequestException() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        newModel.setRoleArn(UPDATE_ROLE_ARN);

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .updateRoleAlias(any(UpdateRoleAliasRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
