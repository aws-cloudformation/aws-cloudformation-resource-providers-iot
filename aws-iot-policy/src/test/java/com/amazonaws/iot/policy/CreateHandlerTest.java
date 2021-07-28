package com.amazonaws.iot.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyRequest;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends PolicyTestBase{
    private CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = defaultModelBuilder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPolicy(any(GetPolicyRequest.class)))
                .thenReturn(TEST_GET_POLICY_RESPONSE);

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
        assertThat(response.getResourceModel().getPolicyDocument()).isEqualTo(convertPolicyDocumentJSONStringToMap(POLICY_DOCUMENT));

        verify(iotClient).createPolicy(any(CreatePolicyRequest.class));
    }


    @Test
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();
        doThrow(ResourceAlreadyExistsException.builder().resourceId(POLICY_NAME).build())
                .when(iotClient)
                .createPolicy(any(CreatePolicyRequest.class));

        assertThrows(CfnAlreadyExistsException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .createPolicy(any(CreatePolicyRequest.class));

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_LimitExceededFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(LimitExceededException.builder().build())
                .when(iotClient)
                .createPolicy(any(CreatePolicyRequest.class));

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(InternalException.builder().build())
                .when(iotClient)
                .createPolicy(any(CreatePolicyRequest.class));

        assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .createPolicy(any(CreatePolicyRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
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
