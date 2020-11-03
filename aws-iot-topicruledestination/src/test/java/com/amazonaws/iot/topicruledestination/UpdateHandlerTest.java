package com.amazonaws.iot.topicruledestination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.UpdateTopicRuleDestinationRequest;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {
    private UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class))).thenReturn(
                GetTopicRuleDestinationResponse.builder().topicRuleDestination(TEST_TOPIC_RULE_DEST).build());
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(),
                proxyClient,
                LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(TEST_REQUEST.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        verify(iotClient).updateTopicRuleDestination(any(UpdateTopicRuleDestinationRequest.class));
    }

    @Test
    public void handleRequest_ResourceConflictException() {
        when(iotClient.updateTopicRuleDestination(any(UpdateTopicRuleDestinationRequest.class))).thenThrow(
                ConflictingResourceUpdateException.builder().build());

        assertThrows(CfnResourceConflictException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);
        });
        verify(iotClient).updateTopicRuleDestination(any(UpdateTopicRuleDestinationRequest.class));
    }
}
