package com.amazonaws.iot.authorizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.AuthorizerDescription;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerResponse;
import software.amazon.awssdk.services.iot.model.UpdateAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.UpdateAuthorizerResponse;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AuthorizerTestBase {
    private final UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        final String newTokenKeyName = "New_Key";
        final String newAuthorizerFunctionArn = AUTHORIZER_FUNCTION_ARN + "2";
        final Map<String, String> newTokenSigningPublicKeys = new HashMap<>();
        final String newStatus = "ACTIVE";

        newModel.setTokenKeyName(newTokenKeyName);
        newModel.setAuthorizerFunctionArn(newAuthorizerFunctionArn);
        newModel.setTokenSigningPublicKeys(newTokenSigningPublicKeys);
        newModel.setStatus(newStatus);

        final DescribeAuthorizerResponse newDescribeResponse = DescribeAuthorizerResponse.builder()
                .authorizerDescription(AuthorizerDescription.builder()
                        .authorizerName(AUTHORIZER_NAME)
                        .signingDisabled(SIGNING_DISABLED)
                        .authorizerArn(AUTHORIZER_ARN)
                        .tokenKeyName(newTokenKeyName)
                        .authorizerFunctionArn(newAuthorizerFunctionArn)
                        .tokenSigningPublicKeys(newTokenSigningPublicKeys)
                        .status(newStatus)
                        .build())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateAuthorizer(any(UpdateAuthorizerRequest.class)))
                .thenReturn(UpdateAuthorizerResponse.builder().build());
        when(iotClient.describeAuthorizer(any(DescribeAuthorizerRequest.class)))
                .thenReturn(newDescribeResponse);

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
        assertThat(response.getResourceModel().getAuthorizerName()).isEqualTo(AUTHORIZER_NAME);
        assertThat(response.getResourceModel().getAuthorizerFunctionArn()).isEqualTo(newAuthorizerFunctionArn);
        assertThat(response.getResourceModel().getTokenSigningPublicKeys()).isEqualTo(newTokenSigningPublicKeys);
        assertThat(response.getResourceModel().getTokenKeyName()).isEqualTo(newTokenKeyName);
        assertThat(response.getResourceModel().getStatus()).isEqualTo(newStatus);

        verify(iotClient).updateAuthorizer(any(UpdateAuthorizerRequest.class));
    }

    @Test
    public void handleRequest_ResourceConflictException() {
        when(iotClient.updateAuthorizer(any(UpdateAuthorizerRequest.class))).thenThrow(ConflictingResourceUpdateException.builder().build());

        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .previousResourceState(model)
                .build();

        assertThrows(CfnResourceConflictException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);
        });
        verify(iotClient).updateAuthorizer(any(UpdateAuthorizerRequest.class));
    }
}
