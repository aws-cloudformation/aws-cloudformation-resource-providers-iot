package com.amazonaws.iot.securityprofile;


import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.RateLimiter;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AttachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DetachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.MetricToRetain;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.UpdateSecurityProfileResponse;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    private static final int MAX_CALLS_PER_SECOND_LIMIT = 5;

    public UpdateHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ResourceModel desiredModel = request.getDesiredResourceState();
        String desiredArn = desiredModel.getSecurityProfileArn();
        if (!StringUtils.isEmpty(desiredArn)) {
            logger.log("Arn cannot be updated, caller setting it to " + desiredArn);
            return ProgressEvent.failed(desiredModel, callbackContext, HandlerErrorCode.InvalidRequest,
                    "Arn cannot be updated.");
        }

        String securityProfileArn;
        try {
            securityProfileArn = updateSecurityProfile(proxy, desiredModel, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        // Security profile targets are managed by separate APIs, not UpdateSecurityProfile.
        try {
            updateTargetAttachments(proxy, desiredModel, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        // Same for tags.
        try {
            updateTags(proxy, request, securityProfileArn, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        desiredModel.setSecurityProfileArn(securityProfileArn);
        return ProgressEvent.defaultSuccessHandler(desiredModel);
    }

    /**
     * @return The security profile's ARN.
     */
    String updateSecurityProfile(AmazonWebServicesClientProxy proxy,
                                 ResourceModel model,
                                 Logger logger) {

        // If the desired template has no behaviors, passing an empty list in the behaviors field
        // is not enough. UpdateSecurityProfile needs us to pass a deleteBehaviors=true flag.
        boolean deleteBehaviors;
        Set<software.amazon.awssdk.services.iot.model.Behavior> behaviorsForRequest;
        if (CollectionUtils.isNullOrEmpty(model.getBehaviors())) {
            deleteBehaviors = true;
            behaviorsForRequest = null;
        } else {
            deleteBehaviors = false;
            behaviorsForRequest = Translator.translateBehaviorSetFromCfnToIot(model.getBehaviors());
        }

        // Same for alertTargets
        boolean deleteAlertTargets;
        Map<String, software.amazon.awssdk.services.iot.model.AlertTarget> alertTargetsForRequest;
        if (CollectionUtils.isNullOrEmpty(model.getAlertTargets())) {
            deleteAlertTargets = true;
            alertTargetsForRequest = null;
        } else {
            deleteAlertTargets = false;
            alertTargetsForRequest = Translator.translateAlertTargetMapFromCfnToIot(model.getAlertTargets());
        }

        // Same for additionalMetricsToRetain
        boolean deleteAdditionalMetricsToRetain;
        Set<MetricToRetain> additionalMetricsV2ForRequest;
        if (CollectionUtils.isNullOrEmpty(model.getAdditionalMetricsToRetainV2())) {
            deleteAdditionalMetricsToRetain = true;
            additionalMetricsV2ForRequest = null;
        } else {
            deleteAdditionalMetricsToRetain = false;
            additionalMetricsV2ForRequest = Translator.translateMetricToRetainSetFromCfnToIot(
                    model.getAdditionalMetricsToRetainV2());
        }

        UpdateSecurityProfileRequest updateRequest = UpdateSecurityProfileRequest.builder()
                .securityProfileName(model.getSecurityProfileName())
                .securityProfileDescription(model.getSecurityProfileDescription())
                .behaviors(behaviorsForRequest)
                .alertTargetsWithStrings(alertTargetsForRequest)
                .additionalMetricsToRetainV2(additionalMetricsV2ForRequest)
                .deleteBehaviors(deleteBehaviors)
                .deleteAlertTargets(deleteAlertTargets)
                .deleteAdditionalMetricsToRetain(deleteAdditionalMetricsToRetain)
                .build();

        UpdateSecurityProfileResponse updateResponse = proxy.injectCredentialsAndInvokeV2(
                updateRequest, iotClient::updateSecurityProfile);
        String arn = updateResponse.securityProfileArn();
        logger.log("Called UpdateSecurityProfile for " + arn);
        return arn;
    }

    void updateTargetAttachments(AmazonWebServicesClientProxy proxy,
                                 ResourceModel model,
                                 Logger logger) {

        String securityProfileName = model.getSecurityProfileName();
        // Note: we're intentionally getting current attachments by calling ListTargetsForSecurityProfile
        // rather than getting the previous state from CFN. This is in order to overwrite out-of-band changes.
        // We have the same behavior in all Device Defender UpdateHandlers with regards to out-of-band updates.
        Set<String> currentTargets = listTargetsForSecurityProfile(proxy, securityProfileName);

        Set<String> desiredTargets;
        if (model.getTargetArns() == null) {
            desiredTargets = Collections.emptySet();
        } else {
            desiredTargets = model.getTargetArns();
        }

        Set<String> targetsToAttach = desiredTargets.stream()
                .filter(target -> !currentTargets.contains(target))
                .collect(Collectors.toSet());
        Set<String> targetsToDetach = currentTargets.stream()
                .filter(target -> !desiredTargets.contains(target))
                .collect(Collectors.toSet());

        // The number of targets can be large, we need to avoid getting throttled.
        RateLimiter rateLimiter = RateLimiter.create(MAX_CALLS_PER_SECOND_LIMIT);

        for (String targetArn : targetsToAttach) {
            rateLimiter.acquire();
            AttachSecurityProfileRequest attachRequest = AttachSecurityProfileRequest.builder()
                    .securityProfileName(securityProfileName)
                    .securityProfileTargetArn(targetArn)
                    .build();
            proxy.injectCredentialsAndInvokeV2(attachRequest, iotClient::attachSecurityProfile);
            logger.log("Attached " + securityProfileName + " to " + targetArn);
        }

        for (String targetArn : targetsToDetach) {
            rateLimiter.acquire();
            DetachSecurityProfileRequest detachRequest = DetachSecurityProfileRequest.builder()
                    .securityProfileName(securityProfileName)
                    .securityProfileTargetArn(targetArn)
                    .build();
            proxy.injectCredentialsAndInvokeV2(detachRequest, iotClient::detachSecurityProfile);
            logger.log("Detached " + securityProfileName + " from " + targetArn);
        }
    }

    void updateTags(AmazonWebServicesClientProxy proxy,
                    ResourceHandlerRequest<ResourceModel> request,
                    String resourceArn,
                    Logger logger) {

        // Note: we're intentionally getting currentTags by calling ListTags rather than getting
        // the previous state from CFN. This is in order to overwrite out-of-band changes.
        // For example, if we used request.getPreviousResourceTags instead of ListTags, if a user added a new tag
        // via TagResource and didn't add it to the template, we wouldn't know about it and wouldn't untag it.
        // Yet we should, otherwise the resource wouldn't equate the template.
        Set<Tag> currentTags = listTags(proxy, resourceArn);

        // getDesiredResourceTags includes model+stack-level tags, reference: https://tinyurl.com/y55mqrnc
        Set<Tag> nullableDesiredTags = Translator.translateTagsFromCfnToIot(request.getDesiredResourceTags());
        Set<Tag> desiredTags = nullableDesiredTags == null ? Collections.emptySet() : nullableDesiredTags;
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // desiredTags.addAll(Translator.translateTagsFromCfnToIot(request.getSystemTags()));

        Set<String> desiredTagKeys = desiredTags.stream()
                .map(Tag::key)
                .collect(Collectors.toSet());

        Set<String> tagKeysToDetach = currentTags.stream()
                .filter(tag -> !desiredTagKeys.contains(tag.key()))
                .map(Tag::key)
                .collect(Collectors.toSet());
        Set<Tag> tagsToAttach = desiredTags.stream()
                .filter(tag -> !currentTags.contains(tag))
                .collect(Collectors.toSet());

        if (!tagsToAttach.isEmpty()) {
            TagResourceRequest tagResourceRequest = TagResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .tags(tagsToAttach)
                    .build();
            proxy.injectCredentialsAndInvokeV2(tagResourceRequest, iotClient::tagResource);
            logger.log("Called TagResource for " + resourceArn);
        }

        if (!tagKeysToDetach.isEmpty()) {
            UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .tagKeys(tagKeysToDetach)
                    .build();
            proxy.injectCredentialsAndInvokeV2(untagResourceRequest, iotClient::untagResource);
            logger.log("Called UntagResource for " + resourceArn);
        }
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    Set<Tag> listTags(AmazonWebServicesClientProxy proxy,
                      String resourceArn) {
        return HandlerUtils.listTags(iotClient, proxy, resourceArn);
    }

    @VisibleForTesting
    Set<String> listTargetsForSecurityProfile(AmazonWebServicesClientProxy proxy,
                                              String securityProfileName) {
        return HandlerUtils.listTargetsForSecurityProfile(
                iotClient, proxy, securityProfileName);
    }
}
