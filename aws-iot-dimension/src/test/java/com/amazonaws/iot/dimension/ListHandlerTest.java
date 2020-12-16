package com.amazonaws.iot.dimension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.ListDimensionsRequest;
import software.amazon.awssdk.services.iot.model.ListDimensionsResponse;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        ListDimensionsRequest expectedRequest = ListDimensionsRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListDimensionsResponse listResponse = ListDimensionsResponse.builder()
                .dimensionNames("dimension1", "dimension2")
                .nextToken("nextToken2")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest), any()))
                .thenReturn(listResponse);

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        assertThat(response.getNextToken()).isEqualTo("nextToken2");
        List<ResourceModel> expectedModels = Arrays.asList(
                ResourceModel.builder().name("dimension1").build(),
                ResourceModel.builder().name("dimension2").build());
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
    }

    @Test
    public void handleRequest_ThrowUnauthorized_VerifyTranslation() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(UnauthorizedException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
