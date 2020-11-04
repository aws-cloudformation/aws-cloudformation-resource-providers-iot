package com.amazonaws.iot.mitigationaction;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateMitigationActionRequest;
import software.amazon.awssdk.services.iot.model.CreateMitigationActionResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.MitigationActionParams;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandler<CallbackContext> {

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

        CreateMitigationActionRequest createRequest = translateToCreateRequest(request);

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
                    model.getMitigationActionArn()));
            // Note: this is necessary even though MitigationActionId is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MitigationActionId is a read-only property and cannot be set.");
        }

        CreateMitigationActionResponse createMitigationActionResponse;
        try {
            createMitigationActionResponse = proxy.injectCredentialsAndInvokeV2(
                    createRequest, iotClient::createMitigationAction);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        model.setMitigationActionArn(createMitigationActionResponse.actionArn());
        model.setMitigationActionId(createMitigationActionResponse.actionId());
        logger.log(String.format("Created %s with actionId %s.", createMitigationActionResponse.actionArn(),
                createMitigationActionResponse.actionId()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private static CreateMitigationActionRequest translateToCreateRequest(ResourceHandlerRequest<ResourceModel> request) {

        ResourceModel model = request.getDesiredResourceState();

        // We don't require the Name field in CFN resource definition.
        // If it's not provided, generate one based on the Logical ID + Idempotency Token
        if (StringUtils.isBlank(model.getActionName())) {
            // TODO: include stack-id in the generated name. Will do it once the new release of the java plugin
            model.setActionName(IdentifierUtils.generateResourceIdentifier(request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken()));
        }

        // getDesiredResourceTags combines the model and stack-level tags.
        // Reference: https://tinyurl.com/y2p8medk
        Map<String, String> tags = request.getDesiredResourceTags();
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // tags.putAll(request.getSystemTags());
        MitigationActionParams actionParams = Translator.translateActionParamsToSdk(model.getActionParams());

        return CreateMitigationActionRequest.builder()
                .actionName(model.getActionName())
                .roleArn(model.getRoleArn())
                .actionParams(actionParams)
                .tags(Translator.translateTagsToSdk(tags))
                .build();
    }
}