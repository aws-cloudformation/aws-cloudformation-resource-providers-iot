package com.amazonaws.iot.scheduledaudit;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateScheduledAuditRequest;
import software.amazon.awssdk.services.iot.model.CreateScheduledAuditResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.HashMap;
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

        CreateScheduledAuditRequest createRequest = translateToCreateRequest(request, logger);

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
        } catch (ResourceAlreadyExistsException e) {
            logger.log(String.format("Resource already exists %s.", model.getScheduledAuditName()));
            throw new CfnAlreadyExistsException(e);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        model.setScheduledAuditArn(createScheduledAuditResponse.scheduledAuditArn());
        logger.log(String.format("Created %s.", createScheduledAuditResponse.scheduledAuditArn()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateScheduledAuditRequest translateToCreateRequest(
            ResourceHandlerRequest<ResourceModel> request,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // Like most services, we don't require an explicit resource name in the template,
        // and, if it's not provided, generate one based on the stack ID and logical ID.
        if (StringUtils.isBlank(model.getScheduledAuditName())) {
            model.setScheduledAuditName(IdentifierUtils.generateResourceIdentifier(
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
                       ResourceModel.TYPE_NAME + " " + model.getScheduledAuditName());
        }

        // Note that the handlers act as pass-through in terms of input validation.
        // We have some validations in the json model, but we delegate deeper checks to the service.
        // If there's an invalid input, we'll rethrow the service's InvalidRequestException with a readable message.
        return CreateScheduledAuditRequest.builder()
                .scheduledAuditName(model.getScheduledAuditName())
                .frequency(model.getFrequency())
                .dayOfMonth(model.getDayOfMonth())
                .dayOfWeek(model.getDayOfWeek())
                .targetCheckNames(model.getTargetCheckNames())
                .tags(Translator.translateTagsToSdk(allTags))
                .build();
    }
}
