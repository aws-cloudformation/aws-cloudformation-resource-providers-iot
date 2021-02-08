package com.amazonaws.iot.mitigationaction;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateMitigationActionRequest;
import software.amazon.awssdk.services.iot.model.CreateMitigationActionResponse;
import software.amazon.awssdk.services.iot.model.MitigationActionParams;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandler<CallbackContext> {

    // Copied value from software.amazon.cloudformation.resource.IdentifierUtils
    private static final int GENERATED_NAME_MAX_LENGTH = 40;

    private final IotClient iotClient;

    public CreateHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        CreateMitigationActionRequest createRequest = translateToCreateRequest(request, logger);

        ResourceModel model = request.getDesiredResourceState();
        if (!StringUtils.isEmpty(model.getMitigationActionArn())) {
            logger.log(String.format("MitigationActionArn is read-only, but the caller passed %s.",
                    model.getMitigationActionArn()));
            // Note: this is necessary even though MitigationActionArn is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MitigationActionArn is a read-only property and cannot be set.");
        }

        if (!StringUtils.isEmpty(model.getMitigationActionId())) {
            logger.log(String.format("MitigationActionId is read-only, but the caller passed %s.",
                    model.getMitigationActionId()));
            // Note: this is necessary even though MitigationActionId is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MitigationActionId is a read-only property and cannot be set.");
        }

        CreateMitigationActionResponse createMitigationActionResponse;
        try {
            createMitigationActionResponse = proxy.injectCredentialsAndInvokeV2(
                    createRequest, iotClient::createMitigationAction);
        } catch (ResourceAlreadyExistsException e) {
            logger.log(String.format("Resource already exists %s.", model.getActionName()));
            throw new CfnAlreadyExistsException(e);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        model.setMitigationActionArn(createMitigationActionResponse.actionArn());
        model.setMitigationActionId(createMitigationActionResponse.actionId());
        logger.log(String.format("Created %s with actionId %s.", createMitigationActionResponse.actionArn(),
                createMitigationActionResponse.actionId()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private static CreateMitigationActionRequest translateToCreateRequest(
            ResourceHandlerRequest<ResourceModel> request,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // Like most services, we don't require an explicit resource name in the template,
        // and, if it's not provided, generate one based on the stack ID and logical ID.
        if (StringUtils.isBlank(model.getActionName())) {
            model.setActionName(IdentifierUtils.generateResourceIdentifier(
                    request.getStackId(), request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(), GENERATED_NAME_MAX_LENGTH));
        }

        // Combine all tags in one map that we'll use for the request
        Map<String, String> allTags = new HashMap<>();
        if (request.getDesiredResourceTags() != null) {
            // DesiredResourceTags includes both model and stack-level tags.
            // Reference: https://tinyurl.com/yyxtd7w6
            allTags.putAll(request.getDesiredResourceTags());
        }
        if (request.getSystemTags() != null) {
            // There are also system tags provided separately.
            // SystemTags are the default stack-level tags with aws:cloudformation prefix
            allTags.putAll(request.getSystemTags());
        } else {
            // System tags should always be present as long as the Handler is called by CloudFormation
            logger.log("Unexpectedly, system tags are null in the create request for " +
                       ResourceModel.TYPE_NAME + " " + model.getActionName());
        }

        MitigationActionParams actionParams = Translator.translateActionParamsToSdk(model.getActionParams());

        // Note that the handlers act as pass-through in terms of input validation.
        // We have some validations in the json model, but we delegate deeper checks to the service.
        // If there's an invalid input, we'll rethrow the service's InvalidRequestException with a readable message.
        return CreateMitigationActionRequest.builder()
                .actionName(model.getActionName())
                .roleArn(model.getRoleArn())
                .actionParams(actionParams)
                .tags(Translator.translateTagsToSdk(allTags))
                .build();
    }
}
