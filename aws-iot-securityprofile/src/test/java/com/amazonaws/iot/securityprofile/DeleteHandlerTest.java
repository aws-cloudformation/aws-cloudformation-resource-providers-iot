package com.amazonaws.iot.securityprofile;

import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.DeleteSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private DeleteHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new DeleteHandler();
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequest() {

        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(2)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        DescribeSecurityProfileRequest describeRequest =
                (DescribeSecurityProfileRequest) requestCaptor.getAllValues().get(0);
        assertThat(describeRequest.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);

        DeleteSecurityProfileRequest deleteRequest =
                (DeleteSecurityProfileRequest) requestCaptor.getAllValues().get(1);
        assertThat(deleteRequest.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);
    }

    @Test
    public void handleRequest_DescribeThrowsNotFound_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_DeleteThrowsException_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(DescribeSecurityProfileResponse.builder().build())
                .thenThrow(InternalFailureException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }
}
