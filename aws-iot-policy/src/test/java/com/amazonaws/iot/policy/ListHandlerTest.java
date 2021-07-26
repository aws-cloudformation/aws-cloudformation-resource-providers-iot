package com.amazonaws.iot.policy;

import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.model.DomainConfigurationSummary;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListDomainConfigurationsResponse;
import software.amazon.awssdk.services.iot.model.ListPoliciesRequest;
import software.amazon.awssdk.services.iot.model.ListPoliciesResponse;
import software.amazon.awssdk.services.iot.model.Policy;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends PolicyTestBase {
    private final ListHandler handler = new ListHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listPolicies(any(ListPoliciesRequest.class))).thenReturn(ListPoliciesResponse.builder()
                .nextMarker(null)
                .policies(Policy.builder()
                        .policyName(POLICY_NAME)
                        .policyArn(POLICY_ARN)
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
        assertThat(model.getPolicyName()).isEqualTo(POLICY_NAME);
        assertThat(model.getArn()).isEqualTo(POLICY_ARN);
        verify(iotClient).listPolicies(any(ListPoliciesRequest.class));
    }

    @Test
    public void handleRequest_PassedNextToken() {
        final ArgumentCaptor<ListPoliciesRequest> policesRequestCaptor = ArgumentCaptor.forClass(ListPoliciesRequest.class);
        final String nextToken = "NEXT";
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null)
                .nextToken(nextToken)
                .build();

        when(iotClient.listPolicies(any(ListPoliciesRequest.class))).thenReturn(ListPoliciesResponse.builder()
                .nextMarker(null)
                .policies(Policy.builder()
                        .policyName(POLICY_NAME)
                        .policyArn(POLICY_ARN)
                        .build())
                .build());

        handler.handleRequest(proxy, request, null, proxyClient, LOGGER);

        verify(iotClient).listPolicies(policesRequestCaptor.capture());
        assertThat(policesRequestCaptor.getValue().marker()).isEqualTo(nextToken);
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        doThrow(InternalFailureException.builder().build())
                .when(iotClient)
                .listPolicies(any(ListPoliciesRequest.class));

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));

    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        doThrow(InvalidRequestException.builder().build())
                .when(iotClient)
                .listPolicies(any(ListPoliciesRequest.class));

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        doThrow(ThrottlingException.builder().build())
                .when(iotClient)
                .listPolicies(any(ListPoliciesRequest.class));

        Assertions.assertThrows(CfnThrottlingException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER));
    }
}
