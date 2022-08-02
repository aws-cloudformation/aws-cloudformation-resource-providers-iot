package com.amazonaws.iot.policy;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iot.model.DeletePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyRequest;
import software.amazon.awssdk.services.iot.model.GetPolicyResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.MalformedPolicyException;
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

import java.util.Map;

public class Translator {
    public static CreatePolicyRequest translateToCreateRequest(ResourceModel model) {
        return CreatePolicyRequest.builder()
                .policyDocument(convertPolicyDocumentMapToJSONString(model.getPolicyDocument()))
                .policyName(model.getPolicyName())
                .build();

    }

    public static GetPolicyRequest translateToReadRequest(ResourceModel model) {
        return GetPolicyRequest.builder()
                .policyName(model.getPolicyName())
                .build();
    }

    public static DeletePolicyRequest translateToDeleteRequest(ResourceModel model) {
        return DeletePolicyRequest.builder()
                .policyName(model.getPolicyName())
                .build();
    }

    public static CreatePolicyVersionRequest translateToCreateVersionRequest(ResourceModel model) {
        return CreatePolicyVersionRequest.builder()
                .policyName(model.getPolicyName())
                .policyDocument(convertPolicyDocumentMapToJSONString(model.getPolicyDocument()))
                .setAsDefault(true)
                .build();
    }



    public static ResourceModel translateFromReadResponse(GetPolicyResponse response) {
        return ResourceModel.builder()
                .arn(response.policyArn())
                .policyName(response.policyName())
                .id(response.policyName())
                .policyDocument(convertPolicyDocumentJSONStringToMap(response.policyDocument()))
                .build();
    }


    public static String convertPolicyDocumentMapToJSONString(Map<String, Object> policyDocumentMap) {
        ObjectMapper policyDocumentMapper = new ObjectMapper();
        try {
            return policyDocumentMapper.writeValueAsString(policyDocumentMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public static Map<String, Object> convertPolicyDocumentJSONStringToMap(final String policyDocument) {
        ObjectMapper policyDocumentMapper = new ObjectMapper();
        try {
            TypeReference<Map<String,Object>> typeRef
                    = new TypeReference<Map<String,Object>>() {};
            return policyDocumentMapper.readValue(policyDocument,  typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static BaseHandlerException translateIotExceptionToHandlerException(
            final IotException e,
            final String operation,
            final String policyName
    ) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(e);
        } else if (e instanceof ResourceNotFoundException) {
            if(StringUtils.isNullOrEmpty(policyName)) return new CfnNotFoundException(e);
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, policyName);
        } else if (e instanceof InvalidRequestException) {
            return new CfnInvalidRequestException(e.getMessage(), e);
        } else if (e instanceof UnauthorizedException) {
            return new CfnAccessDeniedException(operation, e);
        } else if (e instanceof MalformedPolicyException) {
            return new CfnInvalidRequestException(e.getMessage(), e);
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

