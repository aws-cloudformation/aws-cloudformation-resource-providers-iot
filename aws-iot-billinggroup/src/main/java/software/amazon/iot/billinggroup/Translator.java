package software.amazon.iot.billinggroup;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.model.BillingGroupProperties;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListBillingGroupsRequest;
import software.amazon.awssdk.services.iot.model.ListBillingGroupsResponse;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateBillingGroupRequest;
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
        } else if (e instanceof InvalidRequestException) {
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

    static CreateBillingGroupRequest translateToCreateRequest(final ResourceModel model, final Map<String,String> tags) {
        software.amazon.iot.billinggroup.BillingGroupProperties billingGroupProperties =
                software.amazon.iot.billinggroup.BillingGroupProperties.builder().build();
        if (model.getBillingGroupProperties() != null) {
            billingGroupProperties = model.getBillingGroupProperties();
        }
        return CreateBillingGroupRequest.builder()
                .billingGroupName(model.getBillingGroupName())
                .billingGroupProperties(BillingGroupProperties.builder()
                        .billingGroupDescription(billingGroupProperties.getBillingGroupDescription())
                        .build())
                .tags(translateTagsToSdk(tags))
                .build();
    }

    static DescribeBillingGroupRequest translateToReadRequest(final ResourceModel model) {
        return DescribeBillingGroupRequest.builder()
                .billingGroupName(model.getBillingGroupName())
                .build();
    }

    static DeleteBillingGroupRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteBillingGroupRequest.builder()
                .billingGroupName(model.getBillingGroupName())
                .build();
    }

    static UpdateBillingGroupRequest translateToUpdateRequest(final ResourceModel model) {
        software.amazon.iot.billinggroup.BillingGroupProperties billingGroupProperties =
                software.amazon.iot.billinggroup.BillingGroupProperties.builder().billingGroupDescription("").build();
        if (model.getBillingGroupProperties() != null) {
            billingGroupProperties = model.getBillingGroupProperties();
        }
        return UpdateBillingGroupRequest.builder()
                .billingGroupName(model.getBillingGroupName())
                .billingGroupProperties(BillingGroupProperties.builder()
                        .billingGroupDescription(billingGroupProperties.getBillingGroupDescription())
                        .build())
                .build();
    }

    static ListBillingGroupsRequest translateToListRequest(final String nextToken) {
        return ListBillingGroupsRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    static List<ResourceModel> translateFromListResponse(final ListBillingGroupsResponse listBillingGroupsResponse) {
        return streamOfOrEmpty(listBillingGroupsResponse.billingGroups())
                .map(resource -> ResourceModel.builder()
                        .billingGroupName(resource.groupName())
                        .build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    //Translate tags
    static Set<Tag> translateTagsToSdk(final Map<String, String> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }
        return Optional.of(tags.entrySet()).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder()
                        .key(tag.getKey())
                        .value(tag.getValue())
                        .build())
                .collect(Collectors.toSet());
    }

    static Set<software.amazon.iot.billinggroup.Tag> translateTagsFromSdk(final List<Tag> tags) {
        return CollectionUtils.emptyIfNull(tags)
                .stream()
                .map(tag -> software.amazon.iot.billinggroup.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }

    static ListTagsForResourceRequest listResourceTagsRequest(final String resourceArn, final String token) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .nextToken(token)
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

    /**
     * Translates resource object from sdk into a resource model
     * @param describeBillingGroupResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(
            final DescribeBillingGroupResponse describeBillingGroupResponse
    ) {
        return ResourceModel.builder()
                .arn(describeBillingGroupResponse.billingGroupArn())
                .id(describeBillingGroupResponse.billingGroupId())
                .billingGroupName(describeBillingGroupResponse.billingGroupName())
                .billingGroupProperties(translateToModelBillingGroupProperties(describeBillingGroupResponse.billingGroupProperties()))
                .build();
    }

    static software.amazon.iot.billinggroup.BillingGroupProperties translateToModelBillingGroupProperties(
            BillingGroupProperties billingGroupProperties
    ) {
        software.amazon.iot.billinggroup.BillingGroupProperties modelBillingGroupProperties = software.amazon.iot.billinggroup.BillingGroupProperties.builder().build();
        if (billingGroupProperties != null) {
            modelBillingGroupProperties.setBillingGroupDescription(billingGroupProperties.billingGroupDescription());
        }
        return modelBillingGroupProperties;
    }
}
