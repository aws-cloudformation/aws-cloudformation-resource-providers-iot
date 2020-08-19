package com.amazonaws.iot.authorizer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.cloudformation.proxy.StdCallbackContext;

/**
 * Context used for CloudFormation handlers.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

}
