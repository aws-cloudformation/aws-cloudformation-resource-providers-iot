package com.amazonaws.iot.domainconfiguration;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
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

public class ExceptionTranslator {
    static BaseHandlerException translateIotExceptionToHandlerException(
            final IotException e,
            final String operation,
            final String domainConfigurationName
    ) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(e);
        } else if (e instanceof UnauthorizedException) {
            return new CfnAccessDeniedException(operation, e);
        } else if (e instanceof ResourceNotFoundException) {
            if(StringUtils.isNullOrEmpty(domainConfigurationName)) return new CfnNotFoundException(e);
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, domainConfigurationName);
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
}
