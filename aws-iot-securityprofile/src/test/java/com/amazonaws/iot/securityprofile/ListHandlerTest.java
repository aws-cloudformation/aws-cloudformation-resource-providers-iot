package com.amazonaws.iot.securityprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.ListSecurityProfilesRequest;
import software.amazon.awssdk.services.iot.model.ListSecurityProfilesResponse;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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

        ListSecurityProfilesRequest expectedRequest = ListSecurityProfilesRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        software.amazon.awssdk.services.iot.model.SecurityProfileIdentifier identifier1 =
                software.amazon.awssdk.services.iot.model.SecurityProfileIdentifier.builder()
                        .arn("doesn't matter")
                        .name("profile1")
                        .build();
        software.amazon.awssdk.services.iot.model.SecurityProfileIdentifier identifier2 =
                software.amazon.awssdk.services.iot.model.SecurityProfileIdentifier.builder()
                        .arn("doesn't matter")
                        .name("profile2")
                        .build();
        ListSecurityProfilesResponse listResponse = ListSecurityProfilesResponse.builder()
                .securityProfileIdentifiers(identifier1, identifier2)
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
                ResourceModel.builder().securityProfileName("profile1").build(),
                ResourceModel.builder().securityProfileName("profile2").build());
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
    }

    @Test
    public void handleRequest_ApiThrowsException_VerifyTranslation() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(UnauthorizedException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnAccessDeniedException.class);
    }
}
