package software.amazon.iot.thingtype;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.DeleteThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListThingTypesRequest;
import software.amazon.awssdk.services.iot.model.ListThingTypesResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.ThingTypeProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Centralized placeholder for:
 * api request construction
 * object translation to/from aws sdk
 * resource model construction for read/list handlers
 * mapping exceptions to appropriate Cloudformation exceptions
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
        // https://docs.aws.amazon.com/iot/latest/apireference/API_CreateThingType.html#API_CreateThingType_Errors
        // (+same pages for other APIs)
        // For Throttling and InternalFailure, we want CloudFormation to retry, and it will do so based on the error code.
        // Reference with Retryable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof ResourceAlreadyExistsException) {
            return HandlerErrorCode.AlreadyExists;
        } else if (e instanceof software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException) {
            return HandlerErrorCode.AlreadyExists;
        } else if (e instanceof InvalidRequestException) {
            return HandlerErrorCode.InvalidRequest;
        } else if (e instanceof LimitExceededException) {
            return HandlerErrorCode.ServiceLimitExceeded;
        } else if (e instanceof UnauthorizedException) {
            return HandlerErrorCode.AccessDenied;
        } else if (e instanceof InternalFailureException) {
            return HandlerErrorCode.ServiceInternalError;
        } else if (e instanceof InternalException) {
            return HandlerErrorCode.ServiceInternalError;
        } else if (e instanceof ServiceUnavailableException) {
            return HandlerErrorCode.ServiceInternalError;
        } else if (e instanceof ThrottlingException) {
            return HandlerErrorCode.Throttling;
        } else if (e instanceof ResourceNotFoundException) {
            return HandlerErrorCode.NotFound;
        } else if (e instanceof ConflictingResourceUpdateException | e instanceof DeleteConflictException) {
            return HandlerErrorCode.ResourceConflict;
        } else if (e instanceof IotException && ((IotException) e).statusCode() == 403) {
            return HandlerErrorCode.AccessDenied;
        } else {
            logger.log(String.format("Unexpected exception \"%s\", stack trace: %s",
                    e.getMessage(), ExceptionUtils.getStackTrace(e)));
            System.out.println(String.format("Unexpected exception \"%s\", stack trace: %s",
                    e.getMessage(), ExceptionUtils.getStackTrace(e)));
            // Any other exception at this point is unexpected.
            return HandlerErrorCode.InternalFailure;
        }
    }

    static CreateThingTypeRequest translateToCreateRequest(final ResourceModel model, final Map<String,String> tags) {
        software.amazon.iot.thingtype.ThingTypeProperties thingTypeProperties =
                software.amazon.iot.thingtype.ThingTypeProperties.builder().build();
        if (model.getThingTypeProperties() != null) {
            thingTypeProperties = model.getThingTypeProperties();
        }
        return CreateThingTypeRequest.builder()
                .thingTypeName(model.getThingTypeName())
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription(thingTypeProperties.getThingTypeDescription())
                        .searchableAttributes(thingTypeProperties.getSearchableAttributes())
                        .build())
                .tags(translateTagsToSdk(tags))
                .build();
    }

    static DescribeThingTypeRequest translateToReadRequest(final ResourceModel model) {
        return DescribeThingTypeRequest.builder()
                .thingTypeName(model.getThingTypeName())
                .build();
    }

    static DeleteThingTypeRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteThingTypeRequest.builder()
                .thingTypeName(model.getThingTypeName())
                .build();
    }

    static DeprecateThingTypeRequest translateToDeprecateRequest(final ResourceModel model, Boolean deprecateDefault) {
        // deprecateDefault is true for deleteHandler and false for create and update
        boolean deprecate = deprecateDefault;
        if (model.getDeprecateThingType() != null) {
            deprecate = model.getDeprecateThingType();
        }
        return DeprecateThingTypeRequest.builder()
                .thingTypeName(model.getThingTypeName())
                .undoDeprecate(!deprecate)
                .build();
    }

    static ListThingTypesRequest translateToListRequest(final String nextToken) {
        return ListThingTypesRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    static List<ResourceModel> translateFromListRequest(final ListThingTypesResponse listThingTypesResponse) {
        return streamOfOrEmpty(listThingTypesResponse.thingTypes())
                .map(resource -> ResourceModel.builder()
                        .thingTypeName(resource.thingTypeName())
                        .build())
                .collect(Collectors.toList());
    }

    static Set<Tag> translateTagsToSdk(final Map<String, String> tags) {
        if (tags == null) return Collections.emptySet();
        return Optional.of(tags.entrySet()).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static Set<software.amazon.iot.thingtype.Tag> translateTagsFromSdk(final Collection<Tag> tags) {
        return Optional.ofNullable(tags).orElse(Collections.emptySet())
                .stream()
                .map(tag -> software.amazon.iot.thingtype.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }


    static ListTagsForResourceRequest listResourceTagsRequest(final ResourceModel model) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(model.getArn())
                .build();
    }

    static UntagResourceRequest untagResourceRequest(final String arn, final Set<Tag> tags) {
        return UntagResourceRequest.builder()
                .resourceArn(arn)
                .tagKeys(tags
                        .stream()
                        .map(Tag::key)
                        .collect(Collectors.toSet())
                ).build();
    }

    static TagResourceRequest tagResourceRequest(final String arn, final Collection<Tag> tags) {
        return TagResourceRequest.builder()
                .resourceArn(arn)
                .tags(tags).build();
    }
}
