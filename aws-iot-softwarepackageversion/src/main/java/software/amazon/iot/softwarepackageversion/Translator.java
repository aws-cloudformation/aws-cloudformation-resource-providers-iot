package software.amazon.iot.softwarepackageversion;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.iot.cfn.common.handler.Tagging;
import software.amazon.awssdk.services.iot.IotClient;
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
import software.amazon.cloudformation.proxy.ProxyClient;

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
        } else if (e instanceof InvalidRequestException || e instanceof ValidationException) {
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

    static CreatePackageVersionRequest translateToCreateRequest(final ResourceModel model, Map<String, String> combinedTags) {
        if (combinedTags.isEmpty()) {
            return CreatePackageVersionRequest.builder()
                    .packageName(model.getPackageName())
                    .versionName(model.getVersionName())
                    .description(model.getDescription())
                    .attributes(model.getAttributes())
                    .build();
        }
        return CreatePackageVersionRequest.builder()
                .packageName(model.getPackageName())
                .versionName(model.getVersionName())
                .description(model.getDescription())
                .attributes(model.getAttributes())
                .tags(combinedTags)
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
                .build();
    }

    static ListPackageVersionsRequest translateToListRequest(final String packageName, final String nextToken) {
        return ListPackageVersionsRequest.builder()
                .packageName(packageName)
                .nextToken(nextToken)
                .build();
    }

    static ListTagsForResourceRequest translateToListTagsRequestAfterUpdate(final ResourceModel model, final ProxyClient<IotClient> proxyClient, final String operation, Map<String, String> combinedTags) {
        GetPackageVersionRequest getPackageVersionRequest = GetPackageVersionRequest.builder().packageName(model.getPackageName()).versionName(model.getVersionName()).build();
        GetPackageVersionResponse getPackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(
                getPackageVersionRequest, proxyClient.client()::getPackageVersion);
        Tagging.updateResourceTags(getPackageVersionResponse.packageVersionArn(), model.getVersionName(), operation,
                ResourceModel.TYPE_NAME, combinedTags, proxyClient);

        return ListTagsForResourceRequest.builder().resourceArn(getPackageVersionResponse.packageVersionArn()).build();
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

    static Set<software.amazon.iot.softwarepackageversion.Tag> translateTagsToCfn(final List<software.amazon.awssdk.services.iot.model.Tag> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }
        return tags.stream()
                .map(tag -> software.amazon.iot.softwarepackageversion.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }

    static Map<String, String> translateTagsToSdk(
            final Set<software.amazon.iot.softwarepackageversion.Tag> tags) {
        if (tags == null) {
            return Collections.emptyMap();
        }

        return tags.stream()
                .collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }

    static ListTagsForResourceRequest listResourceTagsRequest(final String resourceArn, final String token) {
        return ListTagsForResourceRequest.builder()
                .resourceArn(resourceArn)
                .nextToken(token)
                .build();
    }
}
