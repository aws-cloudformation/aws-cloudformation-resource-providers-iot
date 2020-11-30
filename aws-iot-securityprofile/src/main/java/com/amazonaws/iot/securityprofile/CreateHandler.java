package com.amazonaws.iot.securityprofile;

import java.util.Map;
import java.util.Set;

import com.google.common.util.concurrent.RateLimiter;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AttachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.CreateSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.CreateSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

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

        CreateSecurityProfileRequest createRequest = translateToCreateRequest(request);

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
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
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
                attachSecurityProfile(model.getSecurityProfileName(), targetArn, proxy);
                logger.log("Attached the security profile to " + targetArn);
            }
        }

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateSecurityProfileRequest translateToCreateRequest(
            ResourceHandlerRequest<ResourceModel> request) {

        ResourceModel model = request.getDesiredResourceState();

        // Like most services, we don't require an explicit resource name in the template,
        // and, if it's not provided, generate one based on the stack ID and logical ID.
        if (StringUtils.isBlank(model.getSecurityProfileName())) {
            model.setSecurityProfileName(IdentifierUtils.generateResourceIdentifier(
                    request.getStackId(), request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(), GENERATED_NAME_MAX_LENGTH));
        }

        // getDesiredResourceTags combines the model and stack-level tags.
        // Reference: https://tinyurl.com/yyxtd7w6
        Map<String, String> tags = request.getDesiredResourceTags();
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // tags.putAll(request.getSystemTags());

        // Note that the handlers act as pass-through in terms of input validation.
        // We have some validations in the json model, but we delegate deeper checks to the service.
        // If there's an invalid input, we'll rethrow the service's InvalidRequestException with a readable message.
        return CreateSecurityProfileRequest.builder()
                .securityProfileName(model.getSecurityProfileName())
                .securityProfileDescription(model.getSecurityProfileDescription())
                .behaviors(Translator.translateBehaviorSetFromCfnToIot(model.getBehaviors()))
                .alertTargetsWithStrings(Translator.translateAlertTargetMapFromCfnToIot(model.getAlertTargets()))
                .additionalMetricsToRetain(model.getAdditionalMetricsToRetain())
                .additionalMetricsToRetainV2(Translator.translateMetricToRetainSetFromCfnToIot(
                        model.getAdditionalMetricsToRetainV2()))
                .tags(Translator.translateTagsFromCfnToIot(tags))
                .build();
    }

    private void attachSecurityProfile(String securityProfileName,
                                       String targetArn,
                                       AmazonWebServicesClientProxy proxy) {

        AttachSecurityProfileRequest attachRequest = AttachSecurityProfileRequest.builder()
                .securityProfileName(securityProfileName)
                .securityProfileTargetArn(targetArn)
                .build();
        try {
            proxy.injectCredentialsAndInvokeV2(attachRequest, iotClient::attachSecurityProfile);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }
    }
}
