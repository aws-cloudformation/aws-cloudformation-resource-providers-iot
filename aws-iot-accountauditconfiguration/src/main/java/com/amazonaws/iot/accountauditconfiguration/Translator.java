package com.amazonaws.iot.accountauditconfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class Translator {

    static ProgressEvent<ResourceModel, CallbackContext> translateExceptionToProgressEvent(
            ResourceModel model, Exception e, Logger logger) {

        HandlerErrorCode errorCode = translateExceptionToErrorCode(e, logger);
        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .errorCode(errorCode)
                        .build();
        if (errorCode != HandlerErrorCode.InternalFailure) {
            progressEvent.setMessage(e.getMessage());
        }
        return progressEvent;
    }

    static HandlerErrorCode translateExceptionToErrorCode(Exception e, Logger logger) {

        logger.log(String.format("Translating exception \"%s\", stack trace: %s",
                e.getMessage(), ExceptionUtils.getStackTrace(e)));

        // We're handling all the exceptions documented in API docs
        // https://docs.aws.amazon.com/iot/latest/apireference/API_UpdateAccountAuditConfiguration.html (+similar
        // pages for other APIs)
        // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the exception type.
        // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof InvalidRequestException) {
            return HandlerErrorCode.InvalidRequest;
        } else if (e instanceof UnauthorizedException) {
            return HandlerErrorCode.AccessDenied;
        } else if (e instanceof InternalFailureException) {
            return HandlerErrorCode.InternalFailure;
        } else if (e instanceof ThrottlingException) {
            return HandlerErrorCode.Throttling;
        } else {
            logger.log(String.format("Unexpected exception \"%s\", stack trace: %s",
                    e.getMessage(), ExceptionUtils.getStackTrace(e)));
            // Any other exception at this point is unexpected.
            return HandlerErrorCode.InternalFailure;
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
