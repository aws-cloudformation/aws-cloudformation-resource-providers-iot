package com.amazonaws.iot.policy;

import org.junit.jupiter.api.Assertions;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionResponse;
import software.amazon.awssdk.services.iot.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyRequest;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsResponse;
import software.amazon.awssdk.services.iot.model.PolicyVersion;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DeleteHandlerTest extends PolicyTestBase{

    private DeleteHandler handler = new DeleteHandler();

    private PolicyVersion.Builder defaultVersionBuilder() {
        return PolicyVersion.builder()
                .isDefaultVersion(false)
                .createDate(Instant.now())
                .versionId("1");
    }
    private PolicyVersion.Builder defaultSecondVersionBuilder() {
        return PolicyVersion.builder()
                .isDefaultVersion(false)
                .createDate(Instant.now())
                .versionId("2");
    }


    @Test
    public void handleRequest_SimpleSuccess() {
        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));
        when(iotClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(TEST_GET_POLICY_RESPONSE)
                .thenThrow(ResourceNotFoundException.builder().build());
        final ResourceModel model = ResourceModel.builder().build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

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
    public void handleRequest_DeletePolicyWithAtLeastOnePolicyVersions() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setPolicyDocument(Translator.convertPolicyDocumentJSONStringToMap(UPDATE_POLICY_DOCUMENT));

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.createPolicyVersion(any(CreatePolicyVersionRequest.class)))
                .thenReturn(CreatePolicyVersionResponse.builder().build());

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build(),
                        defaultSecondVersionBuilder().build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        when(iotClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(TEST_GET_POLICY_RESPONSE)
                .thenThrow(ResourceNotFoundException.builder().build());
        final ResourceModel model = ResourceModel.builder().build();

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
    public void handleRequest_ResourceNotFoundFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        doThrow(ResourceNotFoundException.builder().build())
                .when(iotClient)
                .deletePolicy(any(DeletePolicyRequest.class));

        Assertions.assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnInternalFailureException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .deletePolicy(any(DeletePolicyRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnInvalidRequestException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .deletePolicy(any(DeletePolicyRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnGeneralServiceExceptionUnavailable() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        doThrow(ServiceUnavailableException.builder().build())
                .when(iotClient)
                .deletePolicy(any(DeletePolicyRequest.class));

        Assertions.assertThrows(CfnGeneralServiceException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnThrottlingException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .deletePolicy(any(DeletePolicyRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_CfnAccessDeniedException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ListPolicyVersionsResponse listPolicyVersionsResponse = ListPolicyVersionsResponse.builder()
                .policyVersions(
                        defaultVersionBuilder().isDefaultVersion(true).build()
                )
                .build();
        doReturn(listPolicyVersionsResponse)
                .when(iotClient)
                .listPolicyVersions(any(ListPolicyVersionsRequest.class));

        doThrow(UnauthorizedException.builder().build())
                .when(iotClient)
                .deletePolicy(any(DeletePolicyRequest.class));

        Assertions.assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
