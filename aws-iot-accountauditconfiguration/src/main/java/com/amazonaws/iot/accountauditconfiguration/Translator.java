package com.amazonaws.iot.accountauditconfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

public class Translator {

    static BaseHandlerException translateIotExceptionToCfn(IotException e) {

        // We're handling all the exceptions documented in API docs
        // https://docs.aws.amazon.com/iot/latest/apireference/API_UpdateAccountAuditConfiguration.html (+similar
        // pages for other APIs)
        // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the exception type.
        // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof InvalidRequestException) {
            return new CfnInvalidRequestException(e);
        } else if (e instanceof UnauthorizedException) {
            return new CfnAccessDeniedException(e);
        } else if (e instanceof InternalFailureException) {
            return new CfnInternalFailureException(e);
        } else if (e instanceof ThrottlingException) {
            return new CfnThrottlingException(e);
        } else {
            // Any other exception at this point is unexpected. CFN will catch this and convert appropriately.
            // Reference: https://tinyurl.com/y6mphxbn
            throw e;
        }
    }

    static Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> translateChecksFromCfnToIot(ResourceModel model) {

        return model.getAuditCheckConfigurations()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> translateCheckConfigurationFromCfnToIot(e.getValue())));
    }

    static software.amazon.awssdk.services.iot.model.AuditCheckConfiguration translateCheckConfigurationFromCfnToIot(
            AuditCheckConfiguration auditCheckConfiguration) {

        return software.amazon.awssdk.services.iot.model.AuditCheckConfiguration.builder()
                .enabled(auditCheckConfiguration.getEnabled())
                .build();
    }

    static Map<String, AuditCheckConfiguration> translateChecksFromIotToCfn(
            Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> iotMap) {

        return iotMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> AuditCheckConfiguration.builder().enabled(e.getValue().enabled()).build()));
    }

    static Set<String> getEnabledChecksSetFromIotMap(
            Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> checkConfigurationMap) {

        return checkConfigurationMap
                .entrySet()
                .stream()
                .filter(e -> e.getValue().enabled())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    static Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
    translateNotificationsFromCfnToIot(ResourceModel model) {

        if (model.getAuditNotificationTargetConfigurations() == null) {
            return Collections.emptyMap();
        }
        return model.getAuditNotificationTargetConfigurations()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> translateNotificationTargetFromCfnToIot(e.getValue())));
    }

    private static software.amazon.awssdk.services.iot.model.AuditNotificationTarget
    translateNotificationTargetFromCfnToIot(AuditNotificationTarget notificationTarget) {
        return software.amazon.awssdk.services.iot.model.AuditNotificationTarget.builder()
                .enabled(notificationTarget.getEnabled())
                .roleArn(notificationTarget.getRoleArn())
                .targetArn(notificationTarget.getTargetArn())
                .build();
    }

    static Map<String, AuditNotificationTarget> translateNotificationsFromIotToCfn(
            Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> iotMap) {

        if (iotMap == null) {
            return Collections.emptyMap();
        }

        return iotMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> AuditNotificationTarget.builder()
                                .targetArn(e.getValue().targetArn())
                                .roleArn(e.getValue().roleArn())
                                .enabled(e.getValue().enabled())
                                .build()));
    }
}
