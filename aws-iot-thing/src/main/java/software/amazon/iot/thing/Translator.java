package software.amazon.iot.thing;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.model.AttributePayload;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListThingsRequest;
import software.amazon.awssdk.services.iot.model.ListThingsResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateThingRequest;
import software.amazon.awssdk.services.iot.model.VersionConflictException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

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

    static BaseHandlerException translateIotExceptionToHandlerException(
            String resourceIdentifier, String operationName, IotException e) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, resourceIdentifier, e);
        } else if (e instanceof ResourceNotFoundException) {
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceIdentifier, e);
        } else if (e instanceof UnauthorizedException) {
            return new CfnAccessDeniedException(e);
        } else if (e instanceof InternalFailureException) {
            return new CfnInternalFailureException(e);
        } else if (e instanceof ServiceUnavailableException) {
            return new CfnGeneralServiceException(operationName, e);
        } else if (e instanceof InvalidRequestException) {
            return new CfnInvalidRequestException(e);
        } else if (e instanceof ConflictingResourceUpdateException || e instanceof VersionConflictException) {
            return new CfnResourceConflictException(ResourceModel.TYPE_NAME, resourceIdentifier, e.getMessage(), e);
        } else if (e instanceof ThrottlingException) {
            return new CfnThrottlingException(operationName, e);
        } else if (e.statusCode() == HttpStatusCode.FORBIDDEN) {
            return new CfnAccessDeniedException(operationName, e);
        } else {
            return new CfnServiceInternalErrorException(operationName, e);
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

    static List<ResourceModel> translateFromListResponse(final ListThingsResponse listThingsResponse) {
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

    /**
     * Translates resource object from sdk into a resource model
     * @param describeThingResponse the aws service describe resource response
     * @return model resource model
     */
    public static ResourceModel translateFromReadResponse(final DescribeThingResponse describeThingResponse) {
        return ResourceModel.builder()
                .arn(describeThingResponse.thingArn())
                .id(describeThingResponse.thingId())
                .thingName(describeThingResponse.thingName())
                .attributePayload(translateToModelAttributePayload(describeThingResponse.attributes()))
                .build();
    }
}
