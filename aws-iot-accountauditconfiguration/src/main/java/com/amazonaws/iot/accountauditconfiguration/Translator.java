package com.amazonaws.iot.accountauditconfiguration;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        } else if (e instanceof IotException && ((IotException) e).statusCode() == 403) {
            return HandlerErrorCode.AccessDenied;
        } else {
            logger.log(String.format("Unexpected exception \"%s\", stack trace: %s",
                    e.getMessage(), ExceptionUtils.getStackTrace(e)));
            // Any other exception at this point is unexpected.
            return HandlerErrorCode.InternalFailure;
        }
    }

    static Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> translateChecksFromCfnToIot(ResourceModel model) {


        Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> iotMap = new HashMap<>();

        AuditCheckConfigurations auditCheckConfigurations = model.getAuditCheckConfigurations();
        if (auditCheckConfigurations == null) {
            return Collections.emptyMap();
        }

        if (auditCheckConfigurations.getAuthenticatedCognitoRoleOverlyPermissiveCheck() != null) {
            iotMap.put("AUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getAuthenticatedCognitoRoleOverlyPermissiveCheck()));
        }
        if (auditCheckConfigurations.getCaCertificateExpiringCheck() != null) {
            iotMap.put("CA_CERTIFICATE_EXPIRING_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getCaCertificateExpiringCheck()));
        }
        if (auditCheckConfigurations.getCaCertificateKeyQualityCheck() != null) {
            iotMap.put("CA_CERTIFICATE_KEY_QUALITY_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getCaCertificateKeyQualityCheck()));
        }
        if (auditCheckConfigurations.getConflictingClientIdsCheck() != null) {
            iotMap.put("CONFLICTING_CLIENT_IDS_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getConflictingClientIdsCheck()));
        }
        if (auditCheckConfigurations.getDeviceCertificateExpiringCheck() != null) {
            iotMap.put("DEVICE_CERTIFICATE_EXPIRING_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getDeviceCertificateExpiringCheck()));
        }
        if (auditCheckConfigurations.getDeviceCertificateKeyQualityCheck() != null) {
            iotMap.put("DEVICE_CERTIFICATE_KEY_QUALITY_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getDeviceCertificateKeyQualityCheck()));
        }
        if (auditCheckConfigurations.getDeviceCertificateSharedCheck() != null) {
            iotMap.put("DEVICE_CERTIFICATE_SHARED_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getDeviceCertificateSharedCheck()));
        }
        if (auditCheckConfigurations.getIotPolicyOverlyPermissiveCheck() != null) {
            iotMap.put("IOT_POLICY_OVERLY_PERMISSIVE_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getIotPolicyOverlyPermissiveCheck()));
        }
        if (auditCheckConfigurations.getIotRoleAliasAllowsAccessToUnusedServicesCheck() != null) {
            iotMap.put("IOT_ROLE_ALIAS_ALLOWS_ACCESS_TO_UNUSED_SERVICES_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getIotRoleAliasAllowsAccessToUnusedServicesCheck()));
        }
        if (auditCheckConfigurations.getIotRoleAliasOverlyPermissiveCheck() != null) {
            iotMap.put("IOT_ROLE_ALIAS_OVERLY_PERMISSIVE_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getIotRoleAliasOverlyPermissiveCheck()));
        }
        if (auditCheckConfigurations.getLoggingDisabledCheck() != null) {
            iotMap.put("LOGGING_DISABLED_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getLoggingDisabledCheck()));
        }
        if (auditCheckConfigurations.getRevokedCaCertificateStillActiveCheck() != null) {
            iotMap.put("REVOKED_CA_CERTIFICATE_STILL_ACTIVE_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getRevokedCaCertificateStillActiveCheck()));
        }
        if (auditCheckConfigurations.getRevokedDeviceCertificateStillActiveCheck() != null) {
            iotMap.put("REVOKED_DEVICE_CERTIFICATE_STILL_ACTIVE_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getRevokedDeviceCertificateStillActiveCheck()));
        }
        if (auditCheckConfigurations.getUnauthenticatedCognitoRoleOverlyPermissiveCheck() != null) {
            iotMap.put("UNAUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK", translateCheckConfigurationFromCfnToIot(
                    auditCheckConfigurations.getUnauthenticatedCognitoRoleOverlyPermissiveCheck()));
        }

        return iotMap;
    }

    static software.amazon.awssdk.services.iot.model.AuditCheckConfiguration translateCheckConfigurationFromCfnToIot(
            AuditCheckConfiguration auditCheckConfiguration) {

        return software.amazon.awssdk.services.iot.model.AuditCheckConfiguration.builder()
                .enabled(auditCheckConfiguration.getEnabled())
                .build();
    }

    static AuditCheckConfigurations translateChecksFromIotToCfn(
            Map<String, software.amazon.awssdk.services.iot.model.AuditCheckConfiguration> iotMap) {

        AuditCheckConfigurations translation = new AuditCheckConfigurations();

        if (iotMap.containsKey("AUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK")) {
            translation.setAuthenticatedCognitoRoleOverlyPermissiveCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("AUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("CA_CERTIFICATE_EXPIRING_CHECK")) {
            translation.setCaCertificateExpiringCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("CA_CERTIFICATE_EXPIRING_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("CA_CERTIFICATE_KEY_QUALITY_CHECK")) {
            translation.setCaCertificateKeyQualityCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("CA_CERTIFICATE_KEY_QUALITY_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("CONFLICTING_CLIENT_IDS_CHECK")) {
            translation.setConflictingClientIdsCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("CONFLICTING_CLIENT_IDS_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("DEVICE_CERTIFICATE_EXPIRING_CHECK")) {
            translation.setDeviceCertificateExpiringCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("DEVICE_CERTIFICATE_EXPIRING_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("DEVICE_CERTIFICATE_KEY_QUALITY_CHECK")) {
            translation.setDeviceCertificateKeyQualityCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("DEVICE_CERTIFICATE_KEY_QUALITY_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("DEVICE_CERTIFICATE_SHARED_CHECK")) {
            translation.setDeviceCertificateSharedCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("DEVICE_CERTIFICATE_SHARED_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("IOT_POLICY_OVERLY_PERMISSIVE_CHECK")) {
            translation.setIotPolicyOverlyPermissiveCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("IOT_POLICY_OVERLY_PERMISSIVE_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("IOT_ROLE_ALIAS_ALLOWS_ACCESS_TO_UNUSED_SERVICES_CHECK")) {
            translation.setIotRoleAliasAllowsAccessToUnusedServicesCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("IOT_ROLE_ALIAS_ALLOWS_ACCESS_TO_UNUSED_SERVICES_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("IOT_ROLE_ALIAS_OVERLY_PERMISSIVE_CHECK")) {
            translation.setIotRoleAliasOverlyPermissiveCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("IOT_ROLE_ALIAS_OVERLY_PERMISSIVE_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("LOGGING_DISABLED_CHECK")) {
            translation.setLoggingDisabledCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("LOGGING_DISABLED_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("REVOKED_CA_CERTIFICATE_STILL_ACTIVE_CHECK")) {
            translation.setRevokedCaCertificateStillActiveCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("REVOKED_CA_CERTIFICATE_STILL_ACTIVE_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("REVOKED_DEVICE_CERTIFICATE_STILL_ACTIVE_CHECK")) {
            translation.setRevokedDeviceCertificateStillActiveCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("REVOKED_DEVICE_CERTIFICATE_STILL_ACTIVE_CHECK").enabled()).build());
        }
        if (iotMap.containsKey("UNAUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK")) {
            translation.setUnauthenticatedCognitoRoleOverlyPermissiveCheck(
                    AuditCheckConfiguration.builder().enabled(
                            iotMap.get("UNAUTHENTICATED_COGNITO_ROLE_OVERLY_PERMISSIVE_CHECK").enabled()).build());
        }

        return translation;
    }

    static Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget>
    translateNotificationsFromCfnToIot(ResourceModel model) {

        AuditNotificationTargetConfigurations cfnConfigurations = model.getAuditNotificationTargetConfigurations();
        if (cfnConfigurations == null) {
            return Collections.emptyMap();
        }

        Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> iotMap = new HashMap<>();
        if (cfnConfigurations.getSns() != null) {
            iotMap.put("SNS", translateNotificationTargetFromCfnToIot(cfnConfigurations.getSns()));
        }

        return iotMap;
    }

    private static software.amazon.awssdk.services.iot.model.AuditNotificationTarget
    translateNotificationTargetFromCfnToIot(AuditNotificationTarget notificationTarget) {
        return software.amazon.awssdk.services.iot.model.AuditNotificationTarget.builder()
                .enabled(notificationTarget.getEnabled())
                .roleArn(notificationTarget.getRoleArn())
                .targetArn(notificationTarget.getTargetArn())
                .build();
    }

    static AuditNotificationTargetConfigurations translateNotificationsFromIotToCfn(
            Map<String, software.amazon.awssdk.services.iot.model.AuditNotificationTarget> iotMap) {

        if (CollectionUtils.isNullOrEmpty(iotMap) || !iotMap.containsKey("SNS")) {
            return null;
        }

        return AuditNotificationTargetConfigurations.builder()
                .sns(translateNotificationTargetFromIotToCfn(iotMap.get("SNS")))
                .build();
    }

    private static AuditNotificationTarget translateNotificationTargetFromIotToCfn(
            software.amazon.awssdk.services.iot.model.AuditNotificationTarget target) {
        return AuditNotificationTarget.builder()
                .targetArn(target.targetArn())
                .roleArn(target.roleArn())
                .enabled(target.enabled())
                .build();
    }
}
