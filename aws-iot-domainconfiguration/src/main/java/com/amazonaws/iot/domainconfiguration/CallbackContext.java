package com.amazonaws.iot.domainconfiguration;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Context used for CloudFormation handlers when creating/updating/deleting domain configuration request.
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
