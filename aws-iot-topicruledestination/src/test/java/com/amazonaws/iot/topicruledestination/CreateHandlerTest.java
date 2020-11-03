package com.amazonaws.iot.topicruledestination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    private CreateHandler handler = new CreateHandler();

    @Test
    public void testHandleRequest() {
        when(iotClient.createTopicRuleDestination(any(CreateTopicRuleDestinationRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().build());

        assertThrows(CfnAlreadyExistsException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);
        });

        when(iotClient.createTopicRuleDestination(any(CreateTopicRuleDestinationRequest.class))).thenReturn(
                CreateTopicRuleDestinationResponse.builder().topicRuleDestination(TEST_TOPIC_RULE_DEST).build());
        when(iotClient.getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class))).thenReturn(
                GetTopicRuleDestinationResponse.builder().topicRuleDestination(TEST_TOPIC_RULE_DEST).build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getStatus()).isEqualTo(TEST_TOPIC_RULE_DEST.status().name());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(iotClient).getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class));
        verify(iotClient, times(2)).createTopicRuleDestination(any(CreateTopicRuleDestinationRequest.class));
    }
}
