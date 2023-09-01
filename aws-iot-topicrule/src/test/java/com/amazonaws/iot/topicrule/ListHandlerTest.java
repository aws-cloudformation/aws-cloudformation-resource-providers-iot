package com.amazonaws.iot.topicrule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTopicRulesRequest;
import software.amazon.awssdk.services.iot.model.ListTopicRulesResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase {

    private ListHandler handler = new ListHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.listTopicRules(any(ListTopicRulesRequest.class))).thenReturn(
                ListTopicRulesResponse.builder().rules(TEST_TOPIC_RULE_ITEMS).build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, TEST_REQUEST, null, proxyClient, LOGGER);

        assertThat(response, notNullValue());
        assertThat(response.getStatus(), equalTo(OperationStatus.SUCCESS));
        assertThat(response.getCallbackContext(), nullValue());
        assertThat(response.getCallbackDelaySeconds(), is(0));
        assertThat(response.getResourceModel(), nullValue());
        assertThat(response.getResourceModels(), notNullValue());
        assertThat(response.getMessage(), nullValue());
        assertThat(response.getErrorCode(), nullValue());

        List<ResourceModel> models = response.getResourceModels();
        assertThat(models.size(), is(1));

        ResourceModel actualModel = models.get(0);
        ResourceModel expectedModel = TEST_REQUEST.getDesiredResourceState();
        assertThat(actualModel.getRuleName(), equalTo(expectedModel.getRuleName()));
        assertThat(actualModel.getArn(), equalTo(expectedModel.getArn()));
        assertThat(actualModel.getTopicRulePayload().getRuleDisabled(), equalTo(expectedModel.getTopicRulePayload().getRuleDisabled()));
        verify(iotClient).listTopicRules(any(ListTopicRulesRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequestException() {
        when(iotClient.listTopicRules(any(ListTopicRulesRequest.class))).thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, null, proxyClient, LOGGER);
        });
        verify(iotClient).listTopicRules(any(ListTopicRulesRequest.class));
    }
}
