package com.amazonaws.iot.mitigationaction;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.MitigationActionParams;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */
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
        // https://docs.aws.amazon.com/iot/latest/apireference/API_CreateMitigationAction.html#API_CreateMitigationAction_Errors (+same pages for other APIs)
        // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the error code.
        // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof ResourceAlreadyExistsException) {
            // Note regarding idempotency:
            // CreateMitigationAction API allows tags. CFN attaches its own stack level tags with the request. If a MitigationAction
            // is created out of band and then the same request is sent via CFN, API will throw RAEE because the CFN request will have,
            // extra stack level tags. This behavior satisfies the CreateHandler contract.
            return HandlerErrorCode.AlreadyExists;
        } else if (e instanceof InvalidRequestException) {
            return HandlerErrorCode.InvalidRequest;
        } else if (e instanceof LimitExceededException) {
            return HandlerErrorCode.ServiceLimitExceeded;
        } else if (e instanceof UnauthorizedException) {
            return HandlerErrorCode.AccessDenied;
        } else if (e instanceof InternalFailureException) {
            return HandlerErrorCode.InternalFailure;
        } else if (e instanceof ThrottlingException) {
            return HandlerErrorCode.Throttling;
        } else if (e instanceof ResourceNotFoundException) {
            return HandlerErrorCode.NotFound;
        } else {
            logger.log(String.format("Unexpected exception \"%s\", stack trace: %s",
                    e.getMessage(), ExceptionUtils.getStackTrace(e)));
            // Any other exception at this point is unexpected.
            return HandlerErrorCode.InternalFailure;
        }
    }

    static Set<Tag> translateTagsToSdk(Map<String, String> tags) {

        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.keySet().stream()
                .map(key -> Tag.builder()
                        .key(key)
                        .value(tags.get(key))
                        .build())
                .collect(Collectors.toSet());
    }

    static MitigationActionParams translateActionParamsToSdk(ActionParams actionParams) {

        software.amazon.awssdk.services.iot.model.AddThingsToThingGroupParams addThingsToThingGroupParams = null;
        software.amazon.awssdk.services.iot.model.EnableIoTLoggingParams enableIoTLoggingParams = null;
        software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams publishFindingToSnsParams = null;
        software.amazon.awssdk.services.iot.model.ReplaceDefaultPolicyVersionParams replaceDefaultPolicyVersionParams = null;
        software.amazon.awssdk.services.iot.model.UpdateCACertificateParams updateCACertificateParams = null;
        software.amazon.awssdk.services.iot.model.UpdateDeviceCertificateParams updateDeviceCertificateParams = null;

        if (actionParams.getAddThingsToThingGroupParams() != null) {
            addThingsToThingGroupParams =  software.amazon.awssdk.services.iot.model.AddThingsToThingGroupParams.builder()
                    .overrideDynamicGroups(actionParams.getAddThingsToThingGroupParams().getOverrideDynamicGroups())
                    .thingGroupNames(actionParams.getAddThingsToThingGroupParams().getThingGroupNames())
                    .build();
        }
        if (actionParams.getEnableIoTLoggingParams() != null) {
            enableIoTLoggingParams = software.amazon.awssdk.services.iot.model.EnableIoTLoggingParams.builder()
                    .logLevel(actionParams.getEnableIoTLoggingParams().getLogLevel())
                    .roleArnForLogging(actionParams.getEnableIoTLoggingParams().getRoleArnForLogging())
                    .build();
        }
        if (actionParams.getPublishFindingToSnsParams() != null) {
            publishFindingToSnsParams = software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams.builder()
                    .topicArn(actionParams.getPublishFindingToSnsParams().getTopicArn())
                    .build();
        }
        if (actionParams.getReplaceDefaultPolicyVersionParams() != null) {
            replaceDefaultPolicyVersionParams = software.amazon.awssdk.services.iot.model.ReplaceDefaultPolicyVersionParams.builder()
                    .templateName(actionParams.getReplaceDefaultPolicyVersionParams().getTemplateName())
                    .build();
        }
        if (actionParams.getUpdateCACertificateParams() != null) {
            updateCACertificateParams = software.amazon.awssdk.services.iot.model.UpdateCACertificateParams.builder()
                    .action(actionParams.getUpdateCACertificateParams().getAction())
                    .build();
        }
        if (actionParams.getUpdateDeviceCertificateParams() != null) {
            updateDeviceCertificateParams = software.amazon.awssdk.services.iot.model.UpdateDeviceCertificateParams.builder()
                    .action(actionParams.getUpdateDeviceCertificateParams().getAction())
                    .build();
        }
        return MitigationActionParams.builder()
                .addThingsToThingGroupParams(addThingsToThingGroupParams)
                .enableIoTLoggingParams(enableIoTLoggingParams)
                .publishFindingToSnsParams(publishFindingToSnsParams)
                .replaceDefaultPolicyVersionParams(replaceDefaultPolicyVersionParams)
                .updateCACertificateParams(updateCACertificateParams)
                .updateDeviceCertificateParams(updateDeviceCertificateParams)
                .build();

    }

    static ActionParams translateActionParamsToCfn(MitigationActionParams actionParams) {

        AddThingsToThingGroupParams addThingsToThingGroupParams = null;
        EnableIoTLoggingParams enableIoTLoggingParams = null;
        PublishFindingToSnsParams publishFindingToSnsParams = null;
        ReplaceDefaultPolicyVersionParams replaceDefaultPolicyVersionParams = null;
        UpdateCACertificateParams updateCACertificateParams = null;
        UpdateDeviceCertificateParams updateDeviceCertificateParams = null;

        if (actionParams.addThingsToThingGroupParams() != null) {
            addThingsToThingGroupParams =  AddThingsToThingGroupParams.builder()
                    .overrideDynamicGroups(actionParams.addThingsToThingGroupParams().overrideDynamicGroups())
                    .thingGroupNames(new HashSet<>(actionParams.addThingsToThingGroupParams().thingGroupNames()))
                    .build();
        }
        else if (actionParams.enableIoTLoggingParams() != null) {
            enableIoTLoggingParams = EnableIoTLoggingParams.builder()
                    .logLevel(actionParams.enableIoTLoggingParams().logLevelAsString())
                    .roleArnForLogging(actionParams.enableIoTLoggingParams().roleArnForLogging())
                    .build();
        }
        else if (actionParams.publishFindingToSnsParams() != null) {
            publishFindingToSnsParams = PublishFindingToSnsParams.builder()
                    .topicArn(actionParams.publishFindingToSnsParams().topicArn())
                    .build();
        }
        else if (actionParams.replaceDefaultPolicyVersionParams() != null) {
            replaceDefaultPolicyVersionParams = ReplaceDefaultPolicyVersionParams.builder()
                    .templateName(actionParams.replaceDefaultPolicyVersionParams().templateNameAsString())
                    .build();
        }
        else if (actionParams.updateCACertificateParams() != null) {
            updateCACertificateParams = UpdateCACertificateParams.builder()
                    .action(actionParams.updateCACertificateParams().actionAsString())
                    .build();
        }
        else if (actionParams.updateDeviceCertificateParams() != null) {
            updateDeviceCertificateParams = UpdateDeviceCertificateParams.builder()
                    .action(actionParams.updateDeviceCertificateParams().actionAsString())
                    .build();
        }
        return ActionParams.builder()
                .addThingsToThingGroupParams(addThingsToThingGroupParams)
                .enableIoTLoggingParams(enableIoTLoggingParams)
                .publishFindingToSnsParams(publishFindingToSnsParams)
                .replaceDefaultPolicyVersionParams(replaceDefaultPolicyVersionParams)
                .updateCACertificateParams(updateCACertificateParams)
                .updateDeviceCertificateParams(updateDeviceCertificateParams)
                .build();

    }

    static Set<com.amazonaws.iot.mitigationaction.Tag> translateTagsToCfn(
            List<software.amazon.awssdk.services.iot.model.Tag> tags) {

        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.stream()
                .map(tag -> com.amazonaws.iot.mitigationaction.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }
}
