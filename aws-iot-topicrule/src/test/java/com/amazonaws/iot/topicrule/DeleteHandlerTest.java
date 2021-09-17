package com.amazonaws.iot.topicrule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    private DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_InternalException() {
        when(iotClient.deleteTopicRule(any(DeleteTopicRuleRequest.class))).thenThrow(InternalException.builder().build());

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);
        });
        verify(iotClient).deleteTopicRule(any(DeleteTopicRuleRequest.class));
    }

    @Test
    public void handleRequest_InsufficientPermissionForGetTopicRule() {
        when(iotClient.getTopicRule(any(GetTopicRuleRequest.class))).thenThrow(UnauthorizedException.builder().statusCode(403).build());
        when(iotClient.deleteTopicRule(any(DeleteTopicRuleRequest.class))).thenThrow(InternalException.builder().build());

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);
        });
        verify(iotClient).deleteTopicRule(any(DeleteTopicRuleRequest.class));
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.getTopicRule(any(GetTopicRuleRequest.class))).thenReturn(GetTopicRuleResponse.builder().build()).thenThrow(
                UnauthorizedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);

        assertThat(response, notNullValue());
        assertThat(response.getStatus(), equalTo(OperationStatus.SUCCESS));
        assertThat(response.getCallbackDelaySeconds(), is(0));
        assertThat(response.getResourceModel(), nullValue());
        assertThat(response.getResourceModels(), nullValue());
        assertThat(response.getMessage(), nullValue());
        assertThat(response.getErrorCode(), nullValue());
        verify(iotClient).deleteTopicRule(any(DeleteTopicRuleRequest.class));
    }
}
