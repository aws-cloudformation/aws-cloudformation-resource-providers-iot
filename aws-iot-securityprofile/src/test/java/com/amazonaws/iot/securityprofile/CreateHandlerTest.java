package com.amazonaws.iot.securityprofile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.AttachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.AttachSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.CreateSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.CreateSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_CFN;
import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.CLIENT_REQUEST_TOKEN;
import static com.amazonaws.iot.securityprofile.TestConstants.LOGICAL_IDENTIFIER;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_ARN;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_NAME;
import static com.amazonaws.iot.securityprofile.TestConstants.SYSTEM_TAG_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.SYSTEM_TAG_MAP;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_CFN_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_STRINGMAP;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARNS;
import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private CreateHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new CreateHandler();
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceModel model = buildResourceModel();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(LOGICAL_IDENTIFIER)
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(TAG_1_STRINGMAP)
                .systemTags(SYSTEM_TAG_MAP)
                .build();

        CreateSecurityProfileResponse createResponse = CreateSecurityProfileResponse.builder()
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();

        ArgumentCaptor<IotRequest> requestsCaptor = ArgumentCaptor.forClass(IotRequest.class);

        when(proxy.injectCredentialsAndInvokeV2(requestsCaptor.capture(), any()))
                .thenReturn(createResponse)
                .thenReturn(AttachSecurityProfileResponse.builder().build())
                .thenReturn(AttachSecurityProfileResponse.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        List<IotRequest> iotRequests = requestsCaptor.getAllValues();
        CreateSecurityProfileRequest actualCreateRequest = (CreateSecurityProfileRequest) iotRequests.get(0);
        // Order doesn't matter for tags, but they're modeled as a List, thus we have to check field by field.
        assertThat(actualCreateRequest.tags()).containsExactlyInAnyOrder(TAG_1_IOT, SYSTEM_TAG_IOT);
        assertEquals(SECURITY_PROFILE_NAME, actualCreateRequest.securityProfileName());
        assertEquals(ADDITIONAL_METRICS_IOT, actualCreateRequest.additionalMetricsToRetainV2());

        AttachSecurityProfileRequest actualAttachRequest1 = (AttachSecurityProfileRequest) iotRequests.get(1);
        assertThat(actualAttachRequest1.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);
        assertThat(actualAttachRequest1.securityProfileTargetArn()).isIn(TARGET_ARNS);

        AttachSecurityProfileRequest actualAttachRequest2 = (AttachSecurityProfileRequest) iotRequests.get(1);
        assertThat(actualAttachRequest2.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);
        assertThat(actualAttachRequest2.securityProfileTargetArn()).isIn(TARGET_ARNS);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel expectedModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARNS)
                .tags(TAG_1_CFN_SET)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_CreateThrowsAlreadyExists_VerifyTranslation() {

        ResourceModel model = buildResourceModel();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(LOGICAL_IDENTIFIER)
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(TAG_1_STRINGMAP)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceAlreadyExistsException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnAlreadyExistsException.class);
    }

    @Test
    public void handleRequest_AttachThrowsNotFound_VerifyTranslation() {

        ResourceModel model = buildResourceModel();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(LOGICAL_IDENTIFIER)
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(TAG_1_STRINGMAP)
                .build();

        CreateSecurityProfileResponse createResponse = CreateSecurityProfileResponse.builder()
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(createResponse)
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_NoName_GeneratedByHandler() {

        ResourceModel model = ResourceModel.builder()
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARNS)
                .tags(TAG_1_CFN_SET)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("MyResourceName")
                .clientRequestToken("MyToken")
                .desiredResourceTags(TAG_1_STRINGMAP)
                .stackId("arn:aws:cloudformation:us-east-1:123456789012:stack/my-stack-name/" +
                        "084c0bd1-082b-11eb-afdc-0a2fadfa68a5")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(CreateSecurityProfileResponse.builder().build())
                .thenReturn(AttachSecurityProfileResponse.builder().build())
                .thenReturn(AttachSecurityProfileResponse.builder().build());

        handler.handleRequest(proxy, request, null, logger);

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(
                IotRequest.class);
        verify(proxy, times(3)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        CreateSecurityProfileRequest actualCreateRequest = (CreateSecurityProfileRequest) requestCaptor
                .getAllValues().get(0);
        // Can't easily check the randomly generated name. Just making sure it contains part of
        // the logical identifier and the stack name, and some more random characters
        assertThat(actualCreateRequest.securityProfileName()).contains("my-stack");
        assertThat(actualCreateRequest.securityProfileName()).contains("MyRes");
        assertThat(actualCreateRequest.securityProfileName().length() > 20).isTrue();

        AttachSecurityProfileRequest actualAttachRequest = (AttachSecurityProfileRequest) requestCaptor
                .getAllValues().get(1);
        assertThat(actualCreateRequest.securityProfileName()).isEqualTo(actualAttachRequest.securityProfileName());
    }

    @Test
    public void handleRequest_NonEmptyArn_ExpectFailure() {
        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARNS)
                .tags(TAG_1_CFN_SET)
                .securityProfileArn("Arn is read-only")
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier(LOGICAL_IDENTIFIER)
                .clientRequestToken(CLIENT_REQUEST_TOKEN)
                .desiredResourceTags(TAG_1_STRINGMAP)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    private ResourceModel buildResourceModel() {
        return ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARNS)
                .tags(TAG_1_CFN_SET)
                .build();
    }
}
