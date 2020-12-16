package com.amazonaws.iot.dimension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class Translator {

    static ProgressEvent<ResourceModel, CallbackContext> translateExceptionToErrorCode(
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
        // https://docs.aws.amazon.com/iot/latest/apireference/API_CreateSecurityProfile.html
        // (+similar pages for other APIs)
        // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the error code.
        // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof ResourceAlreadyExistsException) {
            // Note regarding idempotency:
            // CreateSecurityProfile API allows tags. CFN attaches its own stack level tags with the request. If a
            // SecurityProfile is created out of band and then the same request is sent via CFN, the API will throw
            // AlreadyExists because the CFN request will contain the stack level tags.
            // This behavior satisfies the CreateHandler contract.
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

    static Set<com.amazonaws.iot.dimension.Tag> translateTagsToCfn(
            List<software.amazon.awssdk.services.iot.model.Tag> tags) {

        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.stream()
                .map(tag -> com.amazonaws.iot.dimension.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }
}
