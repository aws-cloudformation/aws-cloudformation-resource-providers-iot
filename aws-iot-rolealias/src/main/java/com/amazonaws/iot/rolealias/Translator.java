package com.amazonaws.iot.rolealias;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.DeleteRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasRequest;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdateRoleAliasRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
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

public class Translator {
    public static CreateRoleAliasRequest translateToCreateRequest(ResourceModel model) {
        return CreateRoleAliasRequest.builder()
                .roleAlias(model.getRoleAlias())
                .roleArn(model.getRoleArn())
                .build();
    }

    public static DescribeRoleAliasRequest translateToReadRequest(ResourceModel model) {
        return DescribeRoleAliasRequest.builder()
                .roleAlias(model.getRoleAlias())
                .build();
    }

    public static ResourceModel translateFromReadResponse(DescribeRoleAliasResponse response) {
        return ResourceModel.builder()
                .roleAliasArn(response.roleAliasDescription().roleAliasArn())
                .roleAlias(response.roleAliasDescription().roleAlias())
                .roleArn(response.roleAliasDescription().roleArn())
                .credentialDurationSeconds(response.roleAliasDescription().credentialDurationSeconds())
                .build();
    }

    public static DeleteRoleAliasRequest translateToDeleteRequest(ResourceModel model) {
        return DeleteRoleAliasRequest.builder()
                .roleAlias(model.getRoleAlias())
                .build();
    }

    public static UpdateRoleAliasRequest translateToUpdateRequest(ResourceModel model) {
        return UpdateRoleAliasRequest.builder()
                .roleAlias(model.getRoleAlias())
                .credentialDurationSeconds(model.getCredentialDurationSeconds())
                .roleArn(model.getRoleArn())
                .build();
    }

    public static BaseHandlerException translateIotExceptionToHandlerException(
            final IotException e,
            final String operation,
            final String roleAlias
    ) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(e);
        } else if (e instanceof ResourceNotFoundException) {
            if(StringUtils.isNullOrEmpty(roleAlias)) return new CfnNotFoundException(e);
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, roleAlias);
        } else if (e instanceof InvalidRequestException) {
            return new CfnInvalidRequestException(e.getMessage(), e);
        } else if (e instanceof UnauthorizedException) {
            return new CfnAccessDeniedException(operation, e);
        } else if (e instanceof ConflictingResourceUpdateException) {
            return new CfnResourceConflictException(e);
        } else if (e instanceof DeleteConflictException) {
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
}