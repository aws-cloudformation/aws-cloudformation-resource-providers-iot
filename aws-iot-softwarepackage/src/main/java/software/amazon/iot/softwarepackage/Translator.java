package software.amazon.iot.softwarepackage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    static CreatePackageRequest translateToCreateRequest(final ResourceModel model, Map<String, String> combinedTags) {
        if (combinedTags.isEmpty()) {
            return CreatePackageRequest.builder()
                    .packageName(model.getPackageName())
                    .description(model.getDescription())
                    .build();
        }
        return CreatePackageRequest.builder()
                .packageName(model.getPackageName())
                .description(model.getDescription())
                .tags(combinedTags)
                .build();
    }

    static GetPackageRequest translateToReadRequest(final ResourceModel model) {
        return GetPackageRequest.builder()
                .packageName(model.getPackageName())
                .build();
    }

    static DeletePackageRequest translateToDeleteRequest(final ResourceModel model) {
        return DeletePackageRequest.builder()
                .packageName(model.getPackageName())
                .build();
    }

    static ListPackageVersionsRequest translateToListRequestForPackageVersion(final ResourceModel model) {
        return ListPackageVersionsRequest.builder()
                .packageName(model.getPackageName())
                .build();
    }

    static UpdatePackageRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdatePackageRequest.builder()
                .packageName(model.getPackageName())
                .description(model.getDescription())
                .build();
    }

    static ListTagsForResourceRequest translateToListTagsRequestAfterUpdate(final ResourceModel model, final ProxyClient<IotClient> proxyClient, final String operation, Map<String, String> combinedTags) {
        GetPackageRequest getPackageRequest = GetPackageRequest.builder().packageName(model.getPackageName()).build();
        GetPackageResponse getPackageResponse = proxyClient.injectCredentialsAndInvokeV2(
                getPackageRequest, proxyClient.client()::getPackage);
        Tagging.updateResourceTags(getPackageResponse.packageArn(), model.getPackageName(), operation,
                ResourceModel.TYPE_NAME, combinedTags, proxyClient);

        return ListTagsForResourceRequest.builder().resourceArn(getPackageResponse.packageArn()).build();
    }

    static ListPackagesRequest translateToListRequest(final String nextToken) {
        return ListPackagesRequest.builder()
                .nextToken(nextToken)
                .build();
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    static List<ResourceModel> translateFromListResponse(final ListPackagesResponse listPackagesResponse) {
        return streamOfOrEmpty(listPackagesResponse.packageSummaries())
                .map(resource -> ResourceModel.builder()
                        // include only primary identifier
                        .packageName(resource.packageName())
                        .build())
                .collect(Collectors.toList());
    }

     static ResourceModel translateFromReadResponse(final GetPackageResponse getPackageResponse) {
         return ResourceModel.builder()
                 .packageArn(getPackageResponse.packageArn())
                 .packageName(getPackageResponse.packageName())
                 .description(getPackageResponse.description())
                 .build();
    }

    static Set<software.amazon.iot.softwarepackage.Tag> translateTagsToCfn(final List<software.amazon.awssdk.services.iot.model.Tag> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }

        return tags.stream()
                .map(tag -> software.amazon.iot.softwarepackage.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }

    static Map<String, String> translateTagsToSdk(
            final Set<software.amazon.iot.softwarepackage.Tag> tags) {
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
