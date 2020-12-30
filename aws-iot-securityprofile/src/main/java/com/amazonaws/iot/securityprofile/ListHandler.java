package com.amazonaws.iot.securityprofile;

import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListSecurityProfilesRequest;
import software.amazon.awssdk.services.iot.model.ListSecurityProfilesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ListHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ListSecurityProfilesRequest listRequest = ListSecurityProfilesRequest.builder()
                .nextToken(request.getNextToken())
                .build();

        ListSecurityProfilesResponse listSecurityProfilesResponse;
        try {
            listSecurityProfilesResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listSecurityProfiles);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(request.getDesiredResourceState(), e, logger);
        }

        List<ResourceModel> models = listSecurityProfilesResponse.securityProfileIdentifiers().stream()
                .map(identifier -> ResourceModel.builder()
                        .securityProfileName(identifier.name())
                        .build())
                .collect(Collectors.toList());

        logger.log(String.format("Listed %s resources for accountId %s.",
                ResourceModel.TYPE_NAME, request.getAwsAccountId()));

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(listSecurityProfilesResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
