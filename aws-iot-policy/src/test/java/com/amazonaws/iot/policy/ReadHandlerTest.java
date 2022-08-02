package com.amazonaws.iot.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.GetPolicyRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends PolicyTestBase{
    private ReadHandler handler = new ReadHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();


        when(iotClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(GetPolicyResponse.builder()
                .policyArn(POLICY_ARN)
                .policyName(POLICY_NAME)
                .policyDocument(POLICY_DOCUMENT)
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy,
                request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getArn()).isEqualTo(POLICY_ARN);
        assertThat(response.getResourceModel().getPolicyName()).isEqualTo(POLICY_NAME);
        assertThat(response.getResourceModel().getPolicyDocument()).isEqualTo(convertPolicyDocumentJSONStringToMap(POLICY_DOCUMENT));
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(iotClient).getPolicy(any(GetPolicyRequest.class));
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .getPolicy(any(GetPolicyRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .getPolicy(any(GetPolicyRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ResourceNotFoundException.builder().build())
                .when(iotClient)
                .getPolicy(any(GetPolicyRequest.class));

        Assertions.assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .getPolicy(any(GetPolicyRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    private static Map<String, Object> convertPolicyDocumentJSONStringToMap(final String policyDocument) {
        ObjectMapper policyDocumentMapper = new ObjectMapper();
        try {
            TypeReference<Map<String,Object>> typeRef
                    = new TypeReference<Map<String,Object>>() {};
            return policyDocumentMapper.readValue(policyDocument,  typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
