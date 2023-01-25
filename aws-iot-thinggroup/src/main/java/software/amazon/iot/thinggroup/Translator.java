package software.amazon.iot.thinggroup;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.model.AttributePayload;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidQueryException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListThingGroupsRequest;
import software.amazon.awssdk.services.iot.model.ListThingGroupsResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.ThingGroupProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupRequest;
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
            if (e.getMessage()!= null && e.getMessage().contains("AWS IoT Fleet Indexing is not enabled")) {
                return new CfnInvalidRequestException(e);
            }
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

    static CreateThingGroupRequest translateToCreateThingGroupRequest(final ResourceModel model, final Map<String,String> tags) {
        software.amazon.iot.thinggroup.ThingGroupProperties thingGroupProperties =
                new software.amazon.iot.thinggroup.ThingGroupProperties();
        if(model.getThingGroupProperties() != null)
            thingGroupProperties = model.getThingGroupProperties();
        return CreateThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .parentGroupName(model.getParentGroupName())
                .tags(translateTagsToSdk(tags))
                .thingGroupProperties(translateModelThingGroupPropertiesToObject(thingGroupProperties))
                .build();
    }

    static CreateDynamicThingGroupRequest translateToCreateDynamicThingGroupRequest(final ResourceModel model, final Map<String,String> tags) {
        software.amazon.iot.thinggroup.ThingGroupProperties thingGroupProperties =
                new software.amazon.iot.thinggroup.ThingGroupProperties();
        if(model.getThingGroupProperties() != null)
            thingGroupProperties = model.getThingGroupProperties();
        return CreateDynamicThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .tags(translateTagsToSdk(tags))
                .thingGroupProperties(translateModelThingGroupPropertiesToObject(thingGroupProperties))
                .queryString(model.getQueryString())
                .build();
    }

    static ThingGroupProperties translateModelThingGroupPropertiesToObject(
            software.amazon.iot.thinggroup.ThingGroupProperties thingGroupProperties
    ) {
        return ThingGroupProperties.builder()
                .thingGroupDescription(thingGroupProperties.getThingGroupDescription())
                .attributePayload(translateModelAttributePayloadToObject(thingGroupProperties.getAttributePayload()))
                .build();
    }

    static AttributePayload translateModelAttributePayloadToObject(
            software.amazon.iot.thinggroup.AttributePayload attributePayload
    ) {
        software.amazon.iot.thinggroup.AttributePayload attributePayloadForThingGroup =
                new software.amazon.iot.thinggroup.AttributePayload();
        if(attributePayload != null)
            attributePayloadForThingGroup = attributePayload;
        return AttributePayload.builder()
                .attributes(attributePayloadForThingGroup.getAttributes())
                .build();
    }

    static software.amazon.iot.thinggroup.AttributePayload translateAttributePayloadToModelObject(
            AttributePayload newAttributePayload
    ) {
        AttributePayload attributePayload = AttributePayload.builder().build();
        if(newAttributePayload != null)
            attributePayload = newAttributePayload;
        return software.amazon.iot.thinggroup.AttributePayload.builder()
                .attributes(attributePayload.attributes())
                .build();

    }

    static DescribeThingGroupRequest translateToReadRequest(final ResourceModel model) {
        return DescribeThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .build();
    }

    static software.amazon.iot.thinggroup.ThingGroupProperties translateThingGroupPropertiesToModelObject(
            ThingGroupProperties newThingGroupProperties
    ) {
        ThingGroupProperties thingGroupProperties = ThingGroupProperties.builder().build();
        if(newThingGroupProperties != null)
            thingGroupProperties = newThingGroupProperties;
        return software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                .attributePayload(translateAttributePayloadToModelObject(thingGroupProperties.attributePayload()))
                .thingGroupDescription(thingGroupProperties.thingGroupDescription())
                .build();
    }

    static DeleteThingGroupRequest translateToDeleteThingGroupRequest(final ResourceModel model) {
        return DeleteThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .build();
    }

    static DeleteDynamicThingGroupRequest translateToDeleteDynamicThingGroupRequest(final ResourceModel model) {
        return DeleteDynamicThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .build();
    }

    static UpdateThingGroupRequest translateToUpdateThingGroupRequest(final ResourceModel model) {
        software.amazon.iot.thinggroup.ThingGroupProperties thingGroupProperties =
                new software.amazon.iot.thinggroup.ThingGroupProperties();
        if(model.getThingGroupProperties() != null)
            thingGroupProperties = model.getThingGroupProperties();
        return UpdateThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .thingGroupProperties(translateModelThingGroupPropertiesToObject(thingGroupProperties))
                .build();
    }

    static UpdateDynamicThingGroupRequest translateToFirstDynamicThingGroupUpdateRequest(final ResourceModel model) {
        software.amazon.iot.thinggroup.ThingGroupProperties thingGroupProperties =
                new software.amazon.iot.thinggroup.ThingGroupProperties();
        if(model.getThingGroupProperties() != null)
            thingGroupProperties = model.getThingGroupProperties();
        return UpdateDynamicThingGroupRequest.builder()
                .thingGroupName(model.getThingGroupName())
                .thingGroupProperties(translateModelThingGroupPropertiesToObject(thingGroupProperties))
                .queryString(model.getQueryString())
                .build();
    }

    static ListThingGroupsRequest translateToListRequest(final String nextToken) {
        return ListThingGroupsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    static List<ResourceModel> translateFromListResponse(final ListThingGroupsResponse listThingGroupsResponse) {
        return streamOfOrEmpty(listThingGroupsResponse.thingGroups())
                .map(resource -> ResourceModel.builder()
                        .thingGroupName(resource.groupName())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static ListTagsForResourceRequest listResourceTagsRequest(final String resourceArn, final String token) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .nextToken(token)
                .build();
    }

    //Translate tags
    static Set<Tag> translateTagsToSdk(final Map<String, String> tags) {
        if (tags == null) return Collections.emptySet();
        return Optional.of(tags.entrySet()).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.iot.thinggroup.Tag> translateTagsFromSdk(final List<Tag> tags) {
        return CollectionUtils.emptyIfNull(tags)
                .stream()
                .map(tag -> software.amazon.iot.thinggroup.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
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

    static TagResourceRequest tagResourceRequest(final String arn,
                                                 final Collection<Tag> tags) {
        return TagResourceRequest.builder()
                .resourceArn(arn)
                .tags(tags).build();
    }

    /**
     * Translates resource object from sdk into a resource model
     * @param describeThingGroupResponse the aws service describe resource response
     * @return model resource model
     */
    public static ResourceModel translateFromReadResponse(final DescribeThingGroupResponse describeThingGroupResponse) {
        ResourceModel resourceModel = ResourceModel.builder()
                .arn(describeThingGroupResponse.thingGroupArn())
                .id(describeThingGroupResponse.thingGroupId())
                .thingGroupName(describeThingGroupResponse.thingGroupName())
                .thingGroupProperties(Translator.translateThingGroupPropertiesToModelObject(
                        describeThingGroupResponse.thingGroupProperties()))
                .build();

        if (describeThingGroupResponse.queryString() != null) {
            resourceModel.setQueryString(describeThingGroupResponse.queryString());
        }

        if (describeThingGroupResponse.thingGroupMetadata() != null &&
                describeThingGroupResponse.thingGroupMetadata().parentGroupName() != null) {
            resourceModel.setParentGroupName(describeThingGroupResponse.thingGroupMetadata().parentGroupName());
        }

        return resourceModel;
    }
}
