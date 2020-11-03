package com.amazonaws.iot.topicruledestination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {
    private ReadHandler handler = new ReadHandler();

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

        ResourceModel actualModel = response.getResourceModel();
        ResourceModel expectedModel = TEST_REQUEST.getDesiredResourceState();

        assertionOnResourceModels(actualModel, expectedModel);

        verify(iotClient).getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class));
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        when(iotClient.getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class))).thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, new CallbackContext(), proxyClient, LOGGER);
        });
        verify(iotClient).getTopicRuleDestination(any(GetTopicRuleDestinationRequest.class));
    }
}
