package com.amazonaws.iot.topicruledestination;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.services.iot.model.TopicRuleDestinationStatus;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Data
@EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    private TopicRuleDestinationStatus currentStatus;
}
