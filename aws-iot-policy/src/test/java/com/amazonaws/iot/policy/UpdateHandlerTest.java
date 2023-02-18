package com.amazonaws.iot.policy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionResponse;
import software.amazon.awssdk.services.iot.model.GetPolicyRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsResponse;
import software.amazon.awssdk.services.iot.model.PolicyVersion;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.VersionsLimitExceededException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // needed to mock same method with different params
public class UpdateHandlerTest extends PolicyTestBase {
    private UpdateHandler handler = new UpdateHandler();

    private void mockDefaultVersions() {
        doReturn(ListPolicyVersionsResponse.builder()
                .policyVersions(PolicyVersion.builder()
                        .isDefaultVersion(true)
                        .createDate(Instant.now())
                        .versionId("1")
                        .build())
                .build())
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));
    }

    private PolicyVersion.Builder defaultVersionBuilder() {
        return PolicyVersion.builder()
                .isDefaultVersion(false)
                .createDate(Instant.now())
                .versionId("1");
    }

    @Test
    public void handleRequest_updatesPolicyDocument() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        mockDefaultVersions();

        doReturn(CreatePolicyVersionResponse.builder()
                .isDefaultVersion(true)
                .policyArn(POLICY_ARN)
                .policyVersionId("2")
                .build())
                .when(iotClient)
                .createPolicyVersion(any(CreatePolicyVersionRequest.class));

        when(iotClient.getPolicy(any(GetPolicyRequest.class)))
                .thenReturn(GetPolicyResponse.builder()
                        .policyArn(POLICY_ARN)
                        .policyName(POLICY_NAME)
                        .policyDocument(UPDATE_POLICY_DOCUMENT)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getArn()).isEqualTo(POLICY_ARN);
        assertThat(response.getResourceModel().getPolicyName()).isEqualTo(POLICY_NAME);
        assertThat(response.getResourceModel().getPolicyDocument()).isEqualTo(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));
    }

    @Test
    public void handleRequest_deletesOldestVersion() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.createPolicyVersion(any(CreatePolicyVersionRequest.class)))
                .thenThrow(VersionsLimitExceededException.builder().build())
                .thenReturn(CreatePolicyVersionResponse.builder().build());

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build(),
                        defaultVersionBuilder().build(),
                        defaultVersionBuilder().build(),
                        defaultVersionBuilder().build(),
                        defaultVersionBuilder().build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        when(iotClient.getPolicy(any(GetPolicyRequest.class)))
                .thenReturn(GetPolicyResponse.builder()
                        .policyArn(POLICY_ARN)
                        .policyName(POLICY_NAME)
                        .policyDocument(UPDATE_POLICY_DOCUMENT)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getArn()).isEqualTo(POLICY_ARN);
        assertThat(response.getResourceModel().getPolicyName()).isEqualTo(POLICY_NAME);
        assertThat(response.getResourceModel().getPolicyDocument()).isEqualTo(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();


        newModel.setPolicyName("nonExistPolicyName");
        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        mockDefaultVersions();

        when(iotClient.createPolicyVersion(any(CreatePolicyVersionRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(CreatePolicyVersionResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .createPolicyVersion(any(CreatePolicyVersionRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));

    }

    @Test
    public void handleRequest_CfnThrottlingException() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .createPolicyVersion(any(CreatePolicyVersionRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnInvalidRequestException() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .createPolicyVersion(any(CreatePolicyVersionRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

}
