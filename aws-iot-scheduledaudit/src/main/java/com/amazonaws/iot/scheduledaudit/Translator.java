package com.amazonaws.iot.scheduledaudit;

import software.amazon.awssdk.services.iot.model.DayOfWeek;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Collections;
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


  static BaseHandlerException translateIotExceptionToCfn(IotException e) {

    // We're handling all the exceptions documented in API docs
    // https://docs.aws.amazon.com/iot/latest/apireference/API_CreateScheduledAudit.html#API_CreateScheduledAudit_Errors
    // (+same pages for other APIs)
    // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the exception type.
    // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
    if (e instanceof ResourceAlreadyExistsException) {
      // Note regarding idempotency:
      // CreateScheduledAudit API allows tags. CFN attaches its own stack level tags with the request. If a
      // ScheduledAudit is created out of band and then the same request is sent via CFN, API will throw RAEE because
      // the CFN request will have, extra stack level tags. This behavior satisfies the CreateHandler contract.
      return new CfnAlreadyExistsException(e);
    } else if (e instanceof InvalidRequestException) {
      return new CfnInvalidRequestException(e);
    } else if (e instanceof LimitExceededException) {
      return new CfnServiceLimitExceededException(e);
    } else if (e instanceof UnauthorizedException) {
      return new CfnAccessDeniedException(e);
    } else if (e instanceof InternalFailureException) {
      return new CfnInternalFailureException(e);
    } else if (e instanceof ThrottlingException) {
      return new CfnThrottlingException(e);
    } else if (e instanceof ResourceNotFoundException) {
      return new CfnNotFoundException(e);
    } else {
      // Any other exception at this point is unexpected. CFN will catch this and convert appropriately.
      // Reference: https://tinyurl.com/y6mphxbn
      throw e;
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

  static Set<com.amazonaws.iot.scheduledaudit.Tag> translateTagsToCfn(
          List<software.amazon.awssdk.services.iot.model.Tag> tags) {

    if (tags == null) {
      return Collections.emptySet();
    }

    return tags.stream()
            .map(tag -> com.amazonaws.iot.scheduledaudit.Tag.builder()
                    .key(tag.key())
                    .value(tag.value())
                    .build())
            .collect(Collectors.toSet());
  }

/*  static String translateDayOfTheWeekToCfn(DayOfWeek dayOfWeek) {

    return dayOfWeek == null? null : dayOfWeek.toString();
  }*/
}
