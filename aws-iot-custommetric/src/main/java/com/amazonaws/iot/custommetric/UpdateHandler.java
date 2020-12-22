package com.amazonaws.iot.custommetric;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateCustomMetricRequest;
import software.amazon.awssdk.services.iot.model.UpdateCustomMetricResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.List;
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
        String desiredArn = desiredModel.getMetricArn();
        if (!StringUtils.isEmpty(desiredArn)) {
            logger.log(String.format("MetricArn cannot be updated, caller tried setting it to " + desiredArn));
            return ProgressEvent.failed(desiredModel, callbackContext, HandlerErrorCode.InvalidRequest,
                    "MetricArn cannot be updated.");
        }

        String resourceArn;
        try {
            resourceArn = updateCustomMetric(proxy, desiredModel, logger);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        // For an exiting resource, we have to update via TagResource API, UpdateCustomMetric API doesn't take tags.
        try {
            updateTags(proxy, request, resourceArn, logger);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(desiredModel, e, logger);
        }

        logger.log(String.format("Successfully updated %s.", resourceArn));
        desiredModel.setMetricArn(resourceArn);
        return ProgressEvent.defaultSuccessHandler(request.getDesiredResourceState());
    }

    /**
     * @return The action ARN.
     */
    private String updateCustomMetric(AmazonWebServicesClientProxy proxy,
                                      ResourceModel model,
                                      Logger logger) {

        UpdateCustomMetricRequest updateCustomMetricRequest = UpdateCustomMetricRequest.builder()
                .metricName(model.getMetricName())
                .displayName(model.getDisplayName())
                .build();

        UpdateCustomMetricResponse updateCustomMetricResponse =
                proxy.injectCredentialsAndInvokeV2(updateCustomMetricRequest,
                        iotClient::updateCustomMetric);

        String metricArn = updateCustomMetricResponse.metricArn();
        logger.log(String.format("Called UpdateCustomMetric for %s.", metricArn));
        return metricArn;
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

        // getDesiredResourceTags includes model+stack-level tags, reference: https://tinyurl.com/y2p8medk
        Set<Tag> desiredTags = Translator.translateTagsToSdk(request.getDesiredResourceTags());
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // desiredTags.addAll(Translator.translateTagsToSdk(request.getSystemTags()));

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
