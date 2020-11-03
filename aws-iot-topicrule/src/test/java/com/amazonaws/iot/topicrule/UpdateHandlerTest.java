package com.amazonaws.iot.topicrule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.GetTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ReplaceTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.ReplaceTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.TagResourceResponse;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceResponse;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.stream.Collectors;

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
public class UpdateHandlerTest extends AbstractTestBase {
    private UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        when(iotClient.replaceTopicRule(any(ReplaceTopicRuleRequest.class))).thenReturn(ReplaceTopicRuleResponse.builder().build());
        when(iotClient.getTopicRule(any(GetTopicRuleRequest.class))).thenReturn(
                GetTopicRuleResponse.builder().ruleArn(TOPIC_RULE_ARN).rule(TOPIC_RULE).build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(
                ListTagsForResourceResponse.builder().tags(DESIRED_RULE_TAGS.stream().map(tag -> Tag
                        .builder().key(tag.getKey()).value(tag.getValue()).build()).collect(Collectors.toList())).build());
        when(iotClient.untagResource(any(UntagResourceRequest.class))).thenReturn(UntagResourceResponse.builder().build());

        when(iotClient.tagResource(any(TagResourceRequest.class))).thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, TEST_REQUEST,
                new CallbackContext(), proxyClient, LOGGER);
        assertThat(response, notNullValue());
        assertThat(response.getStatus(), equalTo(OperationStatus.SUCCESS));
        assertThat(response.getCallbackDelaySeconds(), is(0));
        assertThat(response.getResourceModels(), nullValue());
        assertThat(response.getMessage(), nullValue());
        assertThat(response.getErrorCode(), nullValue());
        assertionOnResourceModels(response.getResourceModel(), TEST_REQUEST.getDesiredResourceState());
        verify(iotClient).replaceTopicRule(any(ReplaceTopicRuleRequest.class));
    }

    @Test
    public void handleRequest_ResourceConflictException() {
        when(iotClient.replaceTopicRule(any(ReplaceTopicRuleRequest.class))).thenThrow(ConflictingResourceUpdateException.builder().build());

        assertThrows(CfnResourceConflictException.class, () -> {
            handler.handleRequest(proxy, TEST_REQUEST, TEST_CALLBACK, proxyClient, LOGGER);
        });
        verify(iotClient).replaceTopicRule(any(ReplaceTopicRuleRequest.class));
    }
}
