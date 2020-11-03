package com.amazonaws.iot.topicruledestination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
public class DeleteHandlerTest extends AbstractTestBase {
    private DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_InternalException() {
        when(iotClient.deleteTopicRuleDestination(any(DeleteTopicRuleDestinationRequest.class))).thenThrow(
                InternalException.builder().build());

        assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);
        });
        verify(iotClient).deleteTopicRuleDestination(any(DeleteTopicRuleDestinationRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class))).thenReturn(
                GetTopicRuleDestinationResponse.builder().build()).thenThrow(
                UnauthorizedException.builder().build());
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient,
                LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(iotClient).deleteTopicRuleDestination(any(DeleteTopicRuleDestinationRequest.class));
    }
}
