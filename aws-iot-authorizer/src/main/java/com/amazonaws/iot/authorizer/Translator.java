package com.amazonaws.iot.authorizer;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.model.AuthorizerDescription;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DeleteAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateAuthorizerRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Translator {
    static CreateAuthorizerRequest translateToCreateRequest(final ResourceModel model) {
        return CreateAuthorizerRequest.builder()
                .authorizerFunctionArn(model.getAuthorizerFunctionArn())
                .authorizerName(model.getAuthorizerName())
                .tokenKeyName(model.getTokenKeyName())
                .tokenSigningPublicKeys(model.getTokenSigningPublicKeys())
                .status(model.getStatus())
                .tags(getTags(model))
                .signingDisabled(model.getSigningDisabled())
                .build();
    }

    static DeleteAuthorizerRequest translateToDeleteRequest(final ResourceModel model) {
        return DeleteAuthorizerRequest.builder()
                .authorizerName(model.getAuthorizerName())
                .build();
    }

    static DescribeAuthorizerRequest translateToReadRequest(final ResourceModel model) {
        return DescribeAuthorizerRequest.builder()
                .authorizerName(model.getAuthorizerName())
                .build();
    }

    static UpdateAuthorizerRequest translateToUpdateRequest(final ResourceModel model) {
        return UpdateAuthorizerRequest.builder()
                .authorizerName(model.getAuthorizerName())
                .authorizerFunctionArn(model.getAuthorizerFunctionArn())
                .status(model.getStatus())
                .tokenKeyName(model.getTokenKeyName())
                .tokenSigningPublicKeys(model.getTokenSigningPublicKeys())
                .build();
    }

    static ResourceModel translateFromReadResponse(final DescribeAuthorizerResponse response) {
        final AuthorizerDescription description = response.authorizerDescription();
        return ResourceModel.builder()
                .arn(description.authorizerArn())
                .authorizerName(description.authorizerName())
                .authorizerFunctionArn(description.authorizerFunctionArn())
                .status(description.statusAsString())
                .signingDisabled(description.signingDisabled())
                .tokenKeyName(description.tokenKeyName())
                .tokenSigningPublicKeys(description.tokenSigningPublicKeys())
                .build();
    }

    static BaseHandlerException translateIotExceptionToHandlerException(
            final IotException e,
            final String operation,
            final String authorizerName
    ) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(e);
        } else if (e instanceof UnauthorizedException) {
            return new CfnAccessDeniedException(operation, e);
        } else if (e instanceof ResourceNotFoundException) {
            if(StringUtils.isNullOrEmpty(authorizerName)) return new CfnNotFoundException(e);
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, authorizerName);
        } else if (e instanceof InvalidRequestException) {
            return new CfnInvalidRequestException(e.getMessage(), e);
        } else if (e instanceof ConflictingResourceUpdateException) {
            return new CfnResourceConflictException(e);
        } else if (e instanceof ThrottlingException) {
            return new CfnThrottlingException(operation, e);
        } else if (e instanceof ServiceUnavailableException) {
            throw new CfnGeneralServiceException(operation, e);
        } else if (e instanceof LimitExceededException) {
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, e.getMessage());
        } else {
            return new CfnServiceInternalErrorException(operation, e);
        }
    }

    private static Collection<Tag> getTags(ResourceModel model) {
        final List<com.amazonaws.iot.authorizer.Tag> modelTags = model.getTags();
        return Objects.isNull(modelTags)
                ? null
                : modelTags.stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toList());
    }
}
