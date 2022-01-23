package software.amazon.iot.thing;

import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.AttributePayload;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ListThingsResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateThingRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        // https://docs.aws.amazon.com/iot/latest/apireference/API_CreateThing.html#API_CreateThing_Errors
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
            // Any other exception at this point is unexpected.
            return HandlerErrorCode.InternalFailure;
        }
    }

    static CreateThingRequest translateToCreateRequest(final ResourceModel model) {
        software.amazon.iot.thing.AttributePayload attributePayload = new software.amazon.iot.thing.AttributePayload();
        if (model.getAttributePayload() != null) {
            attributePayload = model.getAttributePayload();
        }
        return CreateThingRequest.builder()
                .thingName(model.getThingName())
                .attributePayload(translateAttributesToObject(attributePayload.getAttributes()))
                .build();
    }

    static AttributePayload translateAttributesToObject(Map<String,String> attributeMap) {
        return  AttributePayload.builder()
                .attributes(attributeMap)
                .build();
    }

    static DescribeThingRequest translateToReadRequest(final ResourceModel model) {
        return DescribeThingRequest.builder()
                .thingName(model.getThingName())
                .build();
    }

    static software.amazon.iot.thing.AttributePayload translateToModelAttributePayload(Map<String,String> attributes) {
        software.amazon.iot.thing.AttributePayload modelAttributePayload = software.amazon.iot.thing.AttributePayload.builder().build();
        if(!attributes.isEmpty())
            modelAttributePayload.setAttributes(attributes);
        return modelAttributePayload;
    }

    static DeleteThingRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteThingRequest.builder()
                .thingName(model.getThingName())
                .build();
    }

    static UpdateThingRequest translateToUpdateRequest(final ResourceModel model) {
        software.amazon.iot.thing.AttributePayload attributePayload = new software.amazon.iot.thing.AttributePayload();
        if (model.getAttributePayload() != null) {
            attributePayload = model.getAttributePayload();
        }
        return UpdateThingRequest.builder()
                .thingName(model.getThingName())
                .attributePayload(translateAttributesToObject(attributePayload.getAttributes()))
                .build();
    }

    static ListThingsRequest translateToListRequest(final String nextToken) {
        return ListThingsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    static List<ResourceModel> translateFromListRequest(final ListThingsResponse listThingsResponse) {
        return streamOfOrEmpty(listThingsResponse.things())
                .map(resource -> ResourceModel.builder()
                        // include only primary identifier
                        .thingName(resource.thingName())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}
