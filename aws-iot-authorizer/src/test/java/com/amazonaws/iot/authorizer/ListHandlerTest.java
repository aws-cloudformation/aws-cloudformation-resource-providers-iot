package com.amazonaws.iot.authorizer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.AuthorizerSummary;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListAuthorizersRequest;
import software.amazon.awssdk.services.iot.model.ListAuthorizersResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AuthorizerTestBase {
    private final ListHandler handler = new ListHandler();


    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listAuthorizers(any(ListAuthorizersRequest.class)))
                .thenReturn(ListAuthorizersResponse.builder()
                        .nextMarker(null)
                        .authorizers(AuthorizerSummary.builder()
                                .authorizerName(AUTHORIZER_NAME)
                                .authorizerArn(AUTHORIZER_ARN)
                                .build())
                        .build());


        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        List<ResourceModel> models = response.getResourceModels();
        assertThat(models.size()).isEqualTo(1);

        ResourceModel model = models.get(0);
        assertThat(model.getAuthorizerName()).isEqualTo(AUTHORIZER_NAME);
        assertThat(model.getArn()).isEqualTo(AUTHORIZER_ARN);

        verify(iotClient).listAuthorizers(any(ListAuthorizersRequest.class));
    }

    @Test
    public void handleRequest_PassedNextMarker() {
        final ArgumentCaptor<ListAuthorizersRequest> authorizersRequestCaptor = ArgumentCaptor.forClass(ListAuthorizersRequest.class);
        final String nextMarker = "NEXT";
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).nextToken(nextMarker).build();

        when(iotClient.listAuthorizers(any(ListAuthorizersRequest.class)))
                .thenReturn(ListAuthorizersResponse.builder()
                        .nextMarker(null)
                        .authorizers(AuthorizerSummary.builder()
                                .authorizerName(AUTHORIZER_NAME)
                                .authorizerArn(AUTHORIZER_ARN)
                                .build())
                        .build());

        handler.handleRequest(proxy, request, null, proxyClient, LOGGER);

        verify(iotClient).listAuthorizers(authorizersRequestCaptor.capture());
        assertThat(authorizersRequestCaptor.getValue().marker()).isEqualTo(nextMarker);
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .listAuthorizers(any(ListAuthorizersRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .listAuthorizers(any(ListAuthorizersRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .listAuthorizers(any(ListAuthorizersRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER));
    }
}
