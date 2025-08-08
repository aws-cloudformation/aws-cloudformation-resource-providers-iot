package software.amazon.iot.thingtype;

import lombok.NonNull;
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
import software.amazon.awssdk.services.iot.model.Mqtt5Configuration;
import software.amazon.awssdk.services.iot.model.PropagatingAttribute;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.ThingTypeProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingTypeRequest;
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
import java.util.HashMap;
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

    static CreateThingTypeRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> tags) {
        return CreateThingTypeRequest.builder()
                .thingTypeName(model.getThingTypeName())
                .thingTypeProperties(buildThingTypeProperties(model.getThingTypeProperties(), false))
                .tags(translateTagsToSdk(tags))
                .build();
    }

    private static ThingTypeProperties buildThingTypeProperties(
            software.amazon.iot.thingtype.ThingTypeProperties properties,
            boolean isUpdateRequest) {
        if (properties == null) {
            return null;
        }

        ThingTypeProperties.Builder thingTypePropertiesBuilder = ThingTypeProperties.builder();

        if (!isUpdateRequest) {
            Optional.ofNullable(properties.getThingTypeDescription())
                    .ifPresent(thingTypePropertiesBuilder::thingTypeDescription);

            Optional.ofNullable(properties.getSearchableAttributes())
                    .ifPresent(thingTypePropertiesBuilder::searchableAttributes);
        }

        Optional.ofNullable(properties.getMqtt5Configuration())
                .map(Translator::validateAndAddMqtt5Configuration)
                .ifPresent(thingTypePropertiesBuilder::mqtt5Configuration);

        return thingTypePropertiesBuilder.build();
    }

    private static Mqtt5Configuration validateAndAddMqtt5Configuration(
            @NonNull software.amazon.iot.thingtype.Mqtt5Configuration mqtt5Configuration) {
        List<PropagatingAttribute> propagatingAttributes = Optional.ofNullable(mqtt5Configuration.getPropagatingAttributes())
                .filter(propagatingAttributeList -> !propagatingAttributeList.isEmpty())
                .map(listOfPropagatingAttributes -> listOfPropagatingAttributes.stream()
                        .map(propagatingAttribute -> PropagatingAttribute.builder()
                                .userPropertyKey(propagatingAttribute.getUserPropertyKey())
                                .connectionAttribute(propagatingAttribute.getConnectionAttribute())
                                .thingAttribute(propagatingAttribute.getThingAttribute())
                                .build())
                        .collect(Collectors.toList()))
                .orElse(null);

        return Mqtt5Configuration.builder()
                .propagatingAttributes(propagatingAttributes)
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

    static UpdateThingTypeRequest translateToUpdateThingTypeRequest(final ResourceModel model) {
        return UpdateThingTypeRequest.builder()
                .thingTypeName(model.getThingTypeName())
                .thingTypeProperties(buildThingTypeProperties(model.getThingTypeProperties(), true))
                .build();
    }

    static DeprecateThingTypeRequest translateToDeprecateRequest(final ResourceModel model, Boolean deprecateDefault) {
        // deprecateDefault is true for deleteHandler and false for create and update
        boolean deprecate = deprecateDefault;
        if (!deprecateDefault && model.getDeprecateThingType() != null) {
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
                        .thingTypeProperties(translateThingTypePropertiesToModelObject(resource.thingTypeProperties()))
                        .build())
                .collect(Collectors.toList());
    }

    static Map<String, String> translateTagstoMap(final Set<software.amazon.iot.thingtype.Tag> tags) {
        if (tags == null) {
            return new HashMap<>();
        }

        return tags.stream()
                .collect(Collectors.toMap(
                        software.amazon.iot.thingtype.Tag::getKey,
                        software.amazon.iot.thingtype.Tag::getValue,
                        (existing, replacement) -> existing,  // In case of duplicate keys, keep existing value
                        HashMap::new
                ));
    }

    static Set<Tag> translateTagsToSdk(final Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        return tags.entrySet()
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    static Set<Tag> translateTagsToSdk(final Set<software.amazon.iot.thingtype.Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }

        return tags.stream()
                .map(tag -> Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
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
            ThingTypeProperties thingTypeProperties) {
        if (thingTypeProperties == null)
        {
            return software.amazon.iot.thingtype.ThingTypeProperties.builder().build();
        }
        return software.amazon.iot.thingtype.ThingTypeProperties.builder()
                .thingTypeDescription(thingTypeProperties.thingTypeDescription())
                .searchableAttributes(thingTypeProperties.searchableAttributes())
                .mqtt5Configuration(getMqtt5ConfigurationFromSdk(thingTypeProperties.mqtt5Configuration()))
                .build();
    }

    static software.amazon.iot.thingtype.Mqtt5Configuration getMqtt5ConfigurationFromSdk(
            Mqtt5Configuration mqtt5Configuration) {
        if (mqtt5Configuration == null)
        {
            return software.amazon.iot.thingtype.Mqtt5Configuration.builder()
                    .propagatingAttributes(Collections.emptyList())
                    .build();
        }
        return software.amazon.iot.thingtype.Mqtt5Configuration.builder()
                .propagatingAttributes(getPropagatingAttributesFromSdk(mqtt5Configuration.propagatingAttributes()))
                .build();
    }

    static List<software.amazon.iot.thingtype.PropagatingAttribute> getPropagatingAttributesFromSdk(
            List<PropagatingAttribute> propagatingAttributes) {
        if (propagatingAttributes == null || propagatingAttributes.isEmpty())
        {
            return Collections.emptyList();
        }
        return propagatingAttributes.stream()
                .map(Translator::buildPropagatingAttributeFromSdk)
                .collect(Collectors.toList());
    }

    static software.amazon.iot.thingtype.PropagatingAttribute buildPropagatingAttributeFromSdk(
            PropagatingAttribute sdkPropagatingAttribute) {
        if (sdkPropagatingAttribute == null)
        {
            return software.amazon.iot.thingtype.PropagatingAttribute.builder().build();
        }
        software.amazon.iot.thingtype.PropagatingAttribute.PropagatingAttributeBuilder builder =
                software.amazon.iot.thingtype.PropagatingAttribute.builder()
                        .userPropertyKey(sdkPropagatingAttribute.userPropertyKey());
        String thingAttribute = sdkPropagatingAttribute.thingAttribute();
        if (thingAttribute != null && !thingAttribute.isEmpty()) {
            builder.thingAttribute(thingAttribute);
        } else {
            builder.connectionAttribute(sdkPropagatingAttribute.connectionAttribute());
        }
        return builder.build();
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
