package com.amazonaws.iot.scheduledaudit;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateScheduledAuditRequest;
import software.amazon.awssdk.services.iot.model.CreateScheduledAuditResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

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

        CreateScheduledAuditRequest createRequest = translateToCreateRequest(request);

        ResourceModel model = request.getDesiredResourceState();
        if (!StringUtils.isEmpty(model.getScheduledAuditArn())) {
            logger.log(String.format("ScheduledAuditArn is read-only, but the caller passed %s.",
                    model.getScheduledAuditArn()));
            // Note: this is necessary even though ScheduledAuditArn is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "ScheduledAuditArn is a read-only property and cannot be set.");
        }

        CreateScheduledAuditResponse createScheduledAuditResponse;
        try {
            createScheduledAuditResponse = proxy.injectCredentialsAndInvokeV2(
                    createRequest, iotClient::createScheduledAudit);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        model.setScheduledAuditArn(createScheduledAuditResponse.scheduledAuditArn());
        logger.log(String.format("Created %s.", createScheduledAuditResponse.scheduledAuditArn()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateScheduledAuditRequest translateToCreateRequest(ResourceHandlerRequest<ResourceModel> request) {

        ResourceModel model = request.getDesiredResourceState();

        // We don't require the Name field in CFN resource definition.
        // If it's not provided, generate one based on the Logical ID + Idempotency Token
        if (StringUtils.isBlank(model.getScheduledAuditName())) {
            model.setScheduledAuditName(IdentifierUtils.generateResourceIdentifier(
                    request.getStackId(), request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(), GENERATED_NAME_MAX_LENGTH));
        }

        // getDesiredResourceTags combines the model and stack-level tags.
        // Reference: https://tinyurl.com/y2p8medk
        Map<String, String> tags = request.getDesiredResourceTags();
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // tags.putAll(request.getSystemTags());

        return CreateScheduledAuditRequest.builder()
                .scheduledAuditName(model.getScheduledAuditName())
                .frequency(model.getFrequency())
                .dayOfMonth(model.getDayOfMonth())
                .dayOfWeek(model.getDayOfWeek())
                .targetCheckNames(model.getTargetCheckNames())
                .tags(Translator.translateTagsToSdk(tags))
                .build();
    }
}
