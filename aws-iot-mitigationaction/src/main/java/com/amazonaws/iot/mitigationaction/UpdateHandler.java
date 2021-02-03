package com.amazonaws.iot.mitigationaction;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateMitigationActionRequest;
import software.amazon.awssdk.services.iot.model.UpdateMitigationActionResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

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
        String desiredArn = desiredModel.getMitigationActionArn();
        if (!StringUtils.isEmpty(desiredArn)) {
            logger.log("MitigationActionArn cannot be updated, caller tried setting it to " + desiredArn);
            return ProgressEvent.failed(desiredModel, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MitigationActionArn cannot be updated.");
        }

        String desiredActionId = desiredModel.getMitigationActionId();
        if (!StringUtils.isEmpty(desiredActionId)) {
            logger.log("MitigationActionId cannot be updated, caller tried setting it to " + desiredActionId);
            return ProgressEvent.failed(desiredModel, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MitigationActionId cannot be updated.");
        }

        String resourceArn;
        try {
            resourceArn = updateMitigationAction(proxy, desiredModel, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        // For an exiting resource, we have to update via TagResource API, UpdateMitigationId API doesn't take tags.
        try {
            updateTags(proxy, request, resourceArn, logger);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        logger.log(String.format("Successfully updated %s.", resourceArn));
        return ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState());
    }

    /**
     * @return The action ARN.
     */
    private String updateMitigationAction(AmazonWebServicesClientProxy proxy,
                                          ResourceModel model,
                                          Logger logger) {

        UpdateMitigationActionRequest updateMitigationActionRequest = UpdateMitigationActionRequest.builder()
                .actionName(model.getActionName())
                .roleArn(model.getRoleArn())
                .actionParams(Translator.translateActionParamsToSdk(model.getActionParams()))
                .build();

        UpdateMitigationActionResponse updateMitigationActionResponse =
                proxy.injectCredentialsAndInvokeV2(updateMitigationActionRequest,
                        iotClient::updateMitigationAction);
        String actionArn = updateMitigationActionResponse.actionArn();
        logger.log(String.format("Called UpdateMitigationAction for %s.", actionArn));
        return actionArn;
    }

    @VisibleForTesting
    void updateTags(AmazonWebServicesClientProxy proxy,
                    ResourceHandlerRequest<ResourceModel> request,
                    String resourceArn,
                    Logger logger) {

        // Note: we're intentionally getting currentTags by calling ListTags rather than getting
        // the previous state from CFN. This is in order to overwrite out-of-band changes.
        // For example, if we used request.getPreviousResourceTags instead of ListTags, if a user added a new tag
        // via TagResource and didn't add it to the template, we wouldn't know about it and wouldn't untag it.
        // Yet we should, otherwise the resource wouldn't equate the template.
        Set<Tag> currentTags = listTags(proxy, resourceArn, logger);

        // Combine all tags in one map that we'll use for the request
        Map<String, String> allDesiredTagsMap = new HashMap<>();
        if (request.getDesiredResourceTags() != null) {
            // DesiredResourceTags includes both model and stack-level tags.
            // Reference: https://tinyurl.com/yyxtd7w6
            allDesiredTagsMap.putAll(request.getDesiredResourceTags());
        }
        if (request.getSystemTags() != null) {
            // There are also system tags provided separately.
            // SystemTags are the default stack-level tags with aws:cloudformation prefix.
            allDesiredTagsMap.putAll(request.getSystemTags());
        } else {
            // System tags should never get updated as they are the stack id, stack name,
            // and logical resource id.
            logger.log("Unexpectedly, system tags are null in the update request for " + resourceArn);
        }

        Set<Tag> desiredTags = Translator.translateTagsToSdk(allDesiredTagsMap);
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
            logger.log(String.format("Called TagResource for %s.", resourceArn));
        }

        if (!tagKeysToDetach.isEmpty()) {
            UntagResourceRequest untagResourceRequest = UntagResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .tagKeys(tagKeysToDetach)
                    .build();
            proxy.injectCredentialsAndInvokeV2(untagResourceRequest, iotClient::untagResource);
            logger.log(String.format("Called UntagResource for %s.", resourceArn));
        }
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    Set<Tag> listTags(AmazonWebServicesClientProxy proxy,
                      String resourceArn, Logger logger) {
        List<Tag> tags = HandlerUtils.listTags(iotClient, proxy, resourceArn, logger);
        return new HashSet<>(tags);
    }
}
