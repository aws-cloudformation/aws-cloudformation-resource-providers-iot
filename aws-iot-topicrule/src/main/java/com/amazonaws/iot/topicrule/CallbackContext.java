package com.amazonaws.iot.topicrule;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Data
@EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
}
