package com.amazonaws.iot.cacertificate;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.model.AutoRegistrationStatus;
import software.amazon.awssdk.services.iot.model.CACertificateStatus;
import software.amazon.awssdk.services.iot.model.CertificateStateException;
import software.amazon.awssdk.services.iot.model.CertificateValidationException;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.DeleteCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.DescribeCaCertificateResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.RegisterCaCertificateRequest;
import software.amazon.awssdk.services.iot.model.RegistrationCodeValidationException;
import software.amazon.awssdk.services.iot.model.RegistrationConfig;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateCaCertificateRequest;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.awssdk.services.iot.model.IotException;
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
    public static RegisterCaCertificateRequest translateToCreateRequest(ResourceModel model) {
        Boolean setAsActive = null;
        Boolean allowAutoRegistration = null;
        if (!StringUtils.isNullOrEmpty(model.getStatus()) && model.getStatus().equalsIgnoreCase(CACertificateStatus.ACTIVE.toString())) {
            setAsActive = true;
        }

        if (!StringUtils.isNullOrEmpty(model.getAutoRegistrationStatus()) && model.getAutoRegistrationStatus().equalsIgnoreCase(AutoRegistrationStatus.ENABLE.toString())) {
            allowAutoRegistration = true;
        }

        RegistrationConfig registrationConfig = null;
        if (model.getRegistrationConfig() != null && (!StringUtils.isNullOrEmpty(model.getRegistrationConfig().getRoleArn()) || !StringUtils.isNullOrEmpty(model.getRegistrationConfig().getTemplateBody()))) {
             registrationConfig = RegistrationConfig.builder()
                    .roleArn(model.getRegistrationConfig().getRoleArn())
                    .templateBody(model.getRegistrationConfig().getTemplateBody())
                    .build();
        }

        return RegisterCaCertificateRequest.builder()
                .caCertificate(model.getCACertificatePem())
                .verificationCertificate(model.getVerificationCertificatePem())
                .registrationConfig(registrationConfig)
                .allowAutoRegistration(allowAutoRegistration)
                .setAsActive(setAsActive)
                .build();
    }

    public static DescribeCaCertificateRequest translateToReadRequest(ResourceModel model) {
        return DescribeCaCertificateRequest.builder()
                .certificateId(model.getId())
                .build();
    }

    public static ResourceModel translateFromReadResponse(DescribeCaCertificateResponse response) {
        return ResourceModel.builder()
                .arn(response.certificateDescription().certificateArn())
                .id(response.certificateDescription().certificateId())
                .cACertificatePem(response.certificateDescription().certificatePem())
                .status(response.certificateDescription().statusAsString())
                .autoRegistrationStatus(response.certificateDescription().autoRegistrationStatusAsString())
                .build();
    }

    public static DeleteCaCertificateRequest translateToDeleteRequest(ResourceModel model) {
        return DeleteCaCertificateRequest.builder()
                .certificateId(model.getId())
                .build();
    }

    public static UpdateCaCertificateRequest translateToUpdateRequest(ResourceModel model) {
        RegistrationConfig registrationConfig = null;
        if (model.getRegistrationConfig() != null && (!StringUtils.isNullOrEmpty(model.getRegistrationConfig().getRoleArn()) || !StringUtils.isNullOrEmpty(model.getRegistrationConfig().getTemplateBody()))) {
            registrationConfig = RegistrationConfig.builder()
                    .roleArn(model.getRegistrationConfig().getRoleArn())
                    .templateBody(model.getRegistrationConfig().getTemplateBody())
                    .build();
        }
        Boolean removeAutoRegistration = null;
        if (!StringUtils.isNullOrEmpty(model.getAutoRegistrationStatus()) && model.getAutoRegistrationStatus().equalsIgnoreCase(AutoRegistrationStatus.ENABLE.toString())) {
            removeAutoRegistration = false;
        }

        return UpdateCaCertificateRequest.builder()
                .certificateId(model.getId())
                .newStatus(model.getStatus())
                .newAutoRegistrationStatus(model.getAutoRegistrationStatus())
                .registrationConfig(registrationConfig)
                .removeAutoRegistration(removeAutoRegistration)
                .build();
    }

    public static BaseHandlerException translateIotExceptionToHandlerException(
            final IotException e,
            final String operation,
            final String certificateId
    ) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(e);
        } else if (e instanceof ResourceNotFoundException) {
            if(StringUtils.isNullOrEmpty(certificateId)) return new CfnNotFoundException(e);
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, certificateId);
        } else if (e instanceof InvalidRequestException ||
                e instanceof CertificateValidationException ||
                e instanceof CertificateStateException ||
                e instanceof RegistrationCodeValidationException) {
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
