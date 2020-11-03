package com.amazonaws.iot.topicrule;

import com.google.common.collect.Sets;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ReplaceTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.ReplaceTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class UpdateHandler extends BaseHandlerStd {
    private static final String OPERATION = "ReplaceTopicRule";
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel prevModel = request.getPreviousResourceState() == null ? request.getDesiredResourceState() : request.getPreviousResourceState();
        ResourceModel newModel = request.getDesiredResourceState();

        validatePropertiesAreUpdatable(newModel, prevModel);

        if (StringUtils.isEmpty(newModel.getId())) {
            throw new CfnNotFoundException(InvalidParameterValueException.builder().message("Parameter 'Id' must be provided.").build());
        }
        newModel.setRuleName(prevModel.getRuleName());
        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-IoT-TopicRule::Update", proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToReplaceTopicRuleRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::updateResource)
                                .progress())
                .then(progress -> updateResourceTags(proxy, proxyClient, progress, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, Translator.setResourceIdIfNull(request, newModel), callbackContext,
                        proxyClient, logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private ReplaceTopicRuleResponse updateResource(final ReplaceTopicRuleRequest awsRequest,
                                                    final ProxyClient<IotClient> proxyClient) {
        try {
            ReplaceTopicRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::replaceTopicRule);
            logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, awsRequest.ruleName()));
            return awsResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(awsRequest.ruleName(), OPERATION, e);
        }
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateResourceTags(final AmazonWebServicesClientProxy proxy,
                                                                               final ProxyClient<IotClient> proxyClient,
                                                                               final ProgressEvent<ResourceModel, CallbackContext> progress,
                                                                               final ResourceHandlerRequest<ResourceModel> request) {
        return proxy.initiate("AWS-IoT-TopicRule::Tagging", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getRequest, proxyInvocation) -> {
                    GetTopicRuleResponse getResponse = proxyInvocation.injectCredentialsAndInvokeV2(getRequest,
                            proxyInvocation.client()::getTopicRule);
                    final String arn = getResponse.ruleArn();
                    final Set<Tag> desiredTags = Translator.translateTagsMapToSdk(request.getDesiredResourceTags());
                    final Set<Tag> previousTags = Translator.translateTagsMapToSdk(request.getPreviousResourceTags());
                    final Set<String> tagKeysToRemove = Sets.difference(previousTags, desiredTags).stream().map(Tag::key).collect(toSet());
                    final Set<Tag> tagsToAdd = Sets.difference(desiredTags, previousTags)
                            .stream()
                            .map(t -> Tag.builder().key(t.key()).value(t.value()).build())
                            .collect(Collectors.toSet());
                    if (CollectionUtils.isNotEmpty(tagKeysToRemove)) {
                        proxyInvocation.injectCredentialsAndInvokeV2(UntagResourceRequest.builder().resourceArn(arn).tagKeys(tagKeysToRemove).build(),
                                proxyInvocation.client()::untagResource);
                    }
                    if (CollectionUtils.isNotEmpty(tagsToAdd)) {
                        proxyInvocation.injectCredentialsAndInvokeV2(TagResourceRequest.builder().resourceArn(arn).tags(tagsToAdd).build(),
                                proxyInvocation.client()::tagResource);
                    }
                    return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                })
                .progress();
    }

    private void validatePropertiesAreUpdatable(final ResourceModel newModel, final ResourceModel prevModel) {
        if (!StringUtils.equals(newModel.getRuleName(), prevModel.getRuleName())) {
            throwCfnNotUpdatableException("RuleName");
        } else if (!StringUtils.equals(newModel.getId(), prevModel.getId())) {
            throwCfnNotUpdatableException("Id");
        } else if (!StringUtils.equals(newModel.getArn(), prevModel.getArn())) {
            throwCfnNotUpdatableException("Arn");
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidParameterValueException.builder()
                .message(String.format("Parameter '%s' is not updatable.", propertyName))
                .build());
    }
}
