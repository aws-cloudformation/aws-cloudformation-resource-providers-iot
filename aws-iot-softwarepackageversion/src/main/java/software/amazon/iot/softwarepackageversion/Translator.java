package software.amazon.iot.softwarepackageversion;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iot.model.*;
import software.amazon.awssdk.http.HttpStatusCode;
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

import javax.annotation.Resource;
import java.util.stream.Stream;


public class Translator {

    static BaseHandlerException translateIotExceptionToHandlerException(
            String resourceIdentifier, String operationName, IotException e) {
        if (e instanceof ResourceAlreadyExistsException || e instanceof ConflictException) {
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

    static UpdateIndexingConfigurationRequest translateToUpdateFIRequest(final ResourceModel model) {
        return UpdateIndexingConfigurationRequest.builder()
                .thingIndexingConfiguration(ThingIndexingConfiguration.builder()
                        .thingIndexingMode(ThingIndexingMode.REGISTRY_AND_SHADOW)
                        .namedShadowIndexingMode(NamedShadowIndexingMode.ON)
                        .filter(IndexingFilter.builder().namedShadowNames(Collections.singletonList("$package")).build())
                        .build())
                .build();
    }

    static CreatePackageVersionRequest translateToCreateRequest(final ResourceModel model) {
        return CreatePackageVersionRequest.builder()
                .packageName(model.getPackageName())
                .versionName(model.getVersionName())
                .description(model.getDescription())
                .attributes(model.getAttributes())
                .tags(model.getTags())
                .build();
    }

    static GetPackageVersionRequest translateToReadRequest(final ResourceModel model) {
        return GetPackageVersionRequest.builder()
                .packageName(model.getPackageName())
                .versionName(model.getVersionName())
                .build();
    }

    static DeletePackageVersionRequest translateToDeleteRequest(final ResourceModel model) {
        return DeletePackageVersionRequest.builder()
                .packageName(model.getPackageName())
                .versionName(model.getVersionName())
                .build();
    }

    static UpdatePackageVersionRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdatePackageVersionRequest.builder()
                .packageName(model.getPackageName())
                .versionName(model.getVersionName())
                .description(model.getDescription())
                .attributes(model.getAttributes())
                .action(model.getAction())
                .build();
    }

    static ListPackageVersionsRequest translateToListRequest(final String packageName, final String nextToken) {
        return ListPackageVersionsRequest.builder()
                .packageName(packageName)
                .nextToken(nextToken)
                .build();
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static List<ResourceModel> translateFromListResponse(final ListPackageVersionsResponse listPackageVersionsResponse) {
        return streamOfOrEmpty(listPackageVersionsResponse.packageVersionSummaries())
                .map(resource -> ResourceModel.builder()
                        // include only primary identifier
                        .packageName(resource.packageName())
                        .versionName(resource.versionName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Translates resource object from sdk into a resource model
     * @param getPackageVersionResponse the aws service describe resource response
     * @return model resource model
     */
     static ResourceModel translateFromReadResponse(final GetPackageVersionResponse getPackageVersionResponse) {
         String errorReason = getPackageVersionResponse.errorReason();
         if (errorReason == null) {
             errorReason = "";
         }
        return ResourceModel.builder()
                .packageVersionArn(getPackageVersionResponse.packageVersionArn())
                .packageName(getPackageVersionResponse.packageName())
                .versionName((getPackageVersionResponse.versionName()))
                .description(getPackageVersionResponse.description())
                .attributes(getPackageVersionResponse.attributes()) //check for null?
                .status(getPackageVersionResponse.statusAsString())
                .errorReason(errorReason) //check for null?
                .build();
    }

    static Set<Tag> translateTagsToSdk(final Map<String, String> tags) {

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

    static Map<String, String> translateTagsToCfn(
            final List<Tag> tags) {

        if (tags == null) {
            return Collections.emptyMap();
        }

        return tags.stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }

    static ListTagsForResourceRequest listResourceTagsRequest(final String resourceArn, final String token) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .nextToken(token)
                .build();
    }
}
