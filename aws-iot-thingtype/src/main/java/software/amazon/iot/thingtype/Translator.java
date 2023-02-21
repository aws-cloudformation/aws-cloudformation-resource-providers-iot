package software.amazon.iot.thingtype;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidQueryException;
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
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

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
        } else if (e instanceof InvalidRequestException || e instanceof InvalidQueryException) {
            return new CfnInvalidRequestException(e);
        } else if (e instanceof LimitExceededException) {
            return new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, e.getMessage());
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

    static List<ResourceModel> translateFromListResponse(final ListThingTypesResponse listThingTypesResponse) {
        return streamOfOrEmpty(listThingTypesResponse.thingTypes())
                .map(resource -> ResourceModel.builder()
                        .thingTypeName(resource.thingTypeName())
                        .arn(resource.thingTypeArn())
                        .deprecateThingType(resource.thingTypeMetadata().deprecated())
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


    static ListTagsForResourceRequest listResourceTagsRequest(final String resourceArn, final String nextToken) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .nextToken(nextToken)
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

    static software.amazon.iot.thingtype.ThingTypeProperties translateThingTypePropertiesToModelObject(
            ThingTypeProperties thingTypeProperties
    ) {
        if (thingTypeProperties == null)
        {
            return software.amazon.iot.thingtype.ThingTypeProperties.builder().build();
        } else {
            return software.amazon.iot.thingtype.ThingTypeProperties.builder()
                    .thingTypeDescription(thingTypeProperties.thingTypeDescription())
                    .searchableAttributes(thingTypeProperties.searchableAttributes())
                    .build();
        }
    }

    /**
     * Translates resource object from sdk into a resource model
     * @param describeThingTypeResponse the aws service describe resource response
     * @return model resource model
     */
    public static ResourceModel translateFromReadResponse(final DescribeThingTypeResponse describeThingTypeResponse) {
        ResourceModel resourceModel = ResourceModel.builder()
                .arn(describeThingTypeResponse.thingTypeArn())
                .id(describeThingTypeResponse.thingTypeId())
                .thingTypeName(describeThingTypeResponse.thingTypeName())
                .thingTypeProperties(Translator.translateThingTypePropertiesToModelObject(
                        describeThingTypeResponse.thingTypeProperties()
                ))
                .deprecateThingType(describeThingTypeResponse.thingTypeMetadata().deprecated())
                .build();

        return resourceModel;
    }
}
