package software.amazon.iot.resourcespecificlogging;

import software.amazon.cloudformation.proxy.StdCallbackContext;

/**
 * The CallbackContext will hold details about the current state of the execution.
 * Because handlers are simply calling skyfall APIs, ther is no need specify any detailed state information in this class.
 */
@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
}
