package com.amazonaws.iot.securityprofile;

import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AttachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.CreateSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.CreateSecurityProfileResponse;
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
import java.util.Set;

public class CreateHandler extends BaseHandler<CallbackContext> {

    // Copied value from software.amazon.cloudformation.resource.IdentifierUtils
    private static final int GENERATED_NAME_MAX_LENGTH = 40;
    private static final int MAX_CALLS_PER_SECOND_LIMIT = 5;

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

        CreateSecurityProfileRequest createRequest = translateToCreateRequest(request, logger);

        ResourceModel model = request.getDesiredResourceState();
        if (!StringUtils.isEmpty(model.getSecurityProfileArn())) {
            logger.log(String.format("Arn is read-only, but the caller passed %s.", model.getSecurityProfileArn()));
            // Note: this is necessary even though Arn is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "Arn is a read-only property and cannot be set.");
        }

        CreateSecurityProfileResponse createResponse;
        try {
            createResponse = proxy.injectCredentialsAndInvokeV2(
                    createRequest, iotClient::createSecurityProfile);
        } catch (ResourceAlreadyExistsException e) {
            logger.log(String.format("Resource already exists %s.", model.getSecurityProfileName()));
            throw new CfnAlreadyExistsException(e);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        model.setSecurityProfileArn(createResponse.securityProfileArn());
        logger.log("Created " + createResponse.securityProfileArn());

        // We're letting customers manage Security Profile attachments in the same CFN template,
        // using the TargetArns field. Thus, we need to make an AttachSecurityProfile call for every target.
        Set<String> targetArns = model.getTargetArns();
        if (targetArns != null) {
            // The number of targets can be large, we need to avoid getting throttled.
            RateLimiter rateLimiter = RateLimiter.create(MAX_CALLS_PER_SECOND_LIMIT);
            for (String targetArn : targetArns) {
                rateLimiter.acquire();

                AttachSecurityProfileRequest attachRequest = AttachSecurityProfileRequest.builder()
                        .securityProfileName(model.getSecurityProfileName())
                        .securityProfileTargetArn(targetArn)
                        .build();
                try {
                    proxy.injectCredentialsAndInvokeV2(attachRequest, iotClient::attachSecurityProfile);
                } catch (RuntimeException e) {
                    return Translator.translateExceptionToProgressEvent(model, e, logger);
                }
                logger.log("Attached the security profile to " + targetArn);
            }
        }

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateSecurityProfileRequest translateToCreateRequest(
            ResourceHandlerRequest<ResourceModel> request,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        // Like most services, we don't require an explicit resource name in the template,
        // and, if it's not provided, generate one based on the stack ID and logical ID.
        if (StringUtils.isBlank(model.getSecurityProfileName())) {
            model.setSecurityProfileName(IdentifierUtils.generateResourceIdentifier(
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
                       ResourceModel.TYPE_NAME + " " + model.getSecurityProfileName());
        }

        // Note that the handlers act as pass-through in terms of input validation.
        // We have some validations in the json model, but we delegate deeper checks to the service.
        // If there's an invalid input, we'll rethrow the service's InvalidRequestException with a readable message.
        return CreateSecurityProfileRequest.builder()
                .securityProfileName(model.getSecurityProfileName())
                .securityProfileDescription(model.getSecurityProfileDescription())
                .behaviors(Translator.translateBehaviorSetFromCfnToIot(model.getBehaviors()))
                .alertTargetsWithStrings(Translator.translateAlertTargetMapFromCfnToIot(model.getAlertTargets()))
                .additionalMetricsToRetainV2(Translator.translateMetricToRetainSetFromCfnToIot(
                        model.getAdditionalMetricsToRetainV2()))
                .tags(Translator.translateTagsFromCfnToIot(allTags))
                .build();
    }
}
