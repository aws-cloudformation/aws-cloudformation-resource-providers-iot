package com.amazonaws.iot.rolealias;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.cloudformation.proxy.StdCallbackContext;

/**
 * Context used for CloudFormation handlers. Not used for this resource type.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

}
