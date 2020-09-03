package com.amazonaws.iot.domainconfiguration;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Context used for CloudFormation handlers. Not used for this resource type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackContext {
    private boolean domainConfigurationDisabled;
    private boolean createOrUpdateInProgress;
    private int retryCount;
}
