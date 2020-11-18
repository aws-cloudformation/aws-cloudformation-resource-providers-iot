package com.amazonaws.iot.topicruledestination;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.HttpUrlDestinationSummary;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTopicRuleDestinationsRequest;
import software.amazon.awssdk.services.iot.model.ListTopicRuleDestinationsResponse;
import software.amazon.awssdk.services.iot.model.TopicRuleDestinationSummary;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {
    private ListHandler handler = new ListHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.listTopicRuleDestinations(any(ListTopicRuleDestinationsRequest.class))).thenReturn(
                ListTopicRuleDestinationsResponse.builder()
                        .destinationSummaries(TopicRuleDestinationSummary.builder()
                                .arn(TEST_ARN)
                                .status(TEST_STATUS)
                                .httpUrlSummary(HttpUrlDestinationSummary.builder().confirmationUrl(TEST_CONFIRM_URL).build())
                                .build())
                        .build());
        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, TEST_REQUEST, null, proxyClient, LOGGER);

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

        ResourceModel actualModel = models.get(0);
        ResourceModel expectedModel = TEST_REQUEST.getDesiredResourceState();

        assertionOnResourceModels(actualModel, expectedModel);

        verify(iotClient).listTopicRuleDestinations(any(ListTopicRuleDestinationsRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequestException() {
        when(iotClient.listTopicRuleDestinations(any(ListTopicRuleDestinationsRequest.class))).thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, null, proxyClient, LOGGER);
        });
        verify(iotClient).listTopicRuleDestinations(any(ListTopicRuleDestinationsRequest.class));
    }
}
