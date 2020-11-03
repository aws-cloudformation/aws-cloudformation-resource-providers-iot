package com.amazonaws.iot.topicruledestination;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.DeleteTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.HttpUrlDestinationConfiguration;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListTopicRuleDestinationsRequest;
import software.amazon.awssdk.services.iot.model.ListTopicRuleDestinationsResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.SqlParseException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.TopicRuleDestination;
import software.amazon.awssdk.services.iot.model.TopicRuleDestinationConfiguration;
import software.amazon.awssdk.services.iot.model.TopicRuleDestinationStatus;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateTopicRuleDestinationRequest;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */
public class Translator {

    /**
     * Request to create a resource
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static CreateTopicRuleDestinationRequest translateToCreateRequest(final ResourceModel model) {
        TopicRuleDestinationConfiguration.Builder configurationBuilder = TopicRuleDestinationConfiguration.builder();
        if (ObjectUtils.anyNotNull(model.getHttpUrlProperties()) && StringUtils.isNoneEmpty(model.getHttpUrlProperties().getConfirmationUrl())) {
            configurationBuilder.httpUrlConfiguration(
                    HttpUrlDestinationConfiguration.builder().confirmationUrl(model.getHttpUrlProperties().getConfirmationUrl()).build());
        }
        return CreateTopicRuleDestinationRequest.builder()
                .destinationConfiguration(configurationBuilder.build())
                .build();
    }

    /**
     * Request to read a resource
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static GetTopicRuleDestinationRequest translateToReadRequest(final ResourceModel model) {
        return GetTopicRuleDestinationRequest.builder().arn(model.getArn()).build();
    }

    /**
     * Translates resource object from sdk into a resource model
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetTopicRuleDestinationResponse awsResponse) {
        final TopicRuleDestination dest = awsResponse.topicRuleDestination();
        ResourceModel model = ResourceModel.builder()
                .arn(dest.arn())
                .status(dest.statusAsString())
                .statusReason(dest.statusReason())
                .build();
        if (dest.httpUrlProperties() != null) {
            HttpUrlDestinationSummary httpUrlDestinationSummary = HttpUrlDestinationSummary.builder()
                    .confirmationUrl(dest.httpUrlProperties().confirmationUrl())
                    .build();
            model.setHttpUrlProperties(httpUrlDestinationSummary);
        }
        return model;
    }

    /**
     * Request to delete a resource
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteTopicRuleDestinationRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteTopicRuleDestinationRequest.builder().arn(model.getArn()).build();
    }

    /**
     * Request to update properties of a previously created resource
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static UpdateTopicRuleDestinationRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateTopicRuleDestinationRequest.builder().arn(model.getArn()).status(model.getStatus()).build();
    }

    /**
     * Request to enable a previously created resource
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static UpdateTopicRuleDestinationRequest translateToEnableRequest(final ResourceModel model) {
        return UpdateTopicRuleDestinationRequest.builder().arn(model.getArn()).status(TopicRuleDestinationStatus.ENABLED).build();
    }

    /**
     * Request to list resources
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListTopicRuleDestinationsRequest translateToListRequest(final String nextToken, final int maxResults) {
        return ListTopicRuleDestinationsRequest.builder().nextToken(nextToken).maxResults(maxResults).build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(final ListTopicRuleDestinationsResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.destinationSummaries())
                .map(resource -> {
                    ResourceModel model = ResourceModel.builder()
                            .arn(resource.arn())
                            .status(resource.statusAsString())
                            .statusReason(resource.statusReason())
                            .build();
                    if (resource.httpUrlSummary() != null) {
                        model.setHttpUrlProperties(HttpUrlDestinationSummary.builder()
                                .confirmationUrl(resource.httpUrlSummary().confirmationUrl())
                                .build());
                    }
                    return model;
                }).collect(Collectors.toList());
    }

    static BaseHandlerException translateIotExceptionToHandlerException(String resourceIdentifier, String operationName, IotException e) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, resourceIdentifier, e);
        } else if (e instanceof UnauthorizedException || e instanceof ResourceNotFoundException) {
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceIdentifier, e);
        } else if (e instanceof InternalFailureException) {
            return new CfnInternalFailureException(e);
        } else if (e instanceof ServiceUnavailableException) {
            return new CfnGeneralServiceException(operationName, e);
        } else if (e instanceof InvalidRequestException || e instanceof SqlParseException) {
            return new CfnInvalidRequestException(e);
        } else if (e instanceof ConflictingResourceUpdateException) {
            return new CfnResourceConflictException(ResourceModel.TYPE_NAME, resourceIdentifier, e.getMessage(), e);
        } else if (e instanceof ThrottlingException) {
            return new CfnThrottlingException(operationName, e);
        } else if (e.statusCode() == 403) {
            return new CfnAccessDeniedException(operationName, e);
        } else {
            return new CfnServiceInternalErrorException(operationName, e);
        }
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }
}