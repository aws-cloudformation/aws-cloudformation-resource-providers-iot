package com.amazonaws.iot.topicrule;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.GetTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

public class CreateHandler extends BaseHandlerStd {
    private static final int MAX_RULE_NAME_LENGTH = 256;
    private static final String OPERATION = "CreateTopicRule";

    private Logger logger;
    private ResourceHandlerRequest<ResourceModel> request;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        this.request = request;

        ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getRuleName())) {
            model.setRuleName(generateName(request));
        }
        final Map<String, String> desiredTags = request.getDesiredResourceTags();
        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-IoT-TopicRule::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(resourceModel -> Translator.translateToCreateRequest(resourceModel, desiredTags))
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::createResource)
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(model));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateTopicRuleResponse createResource(final CreateTopicRuleRequest awsRequest, final ProxyClient<IotClient> proxyClient) {
        try {
            tryGetTopicRule(awsRequest.ruleName(), proxyClient);
            final CreateTopicRuleResponse response = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createTopicRule);
            logger.log(String.format("%s [%s] successfully created.", ResourceModel.TYPE_NAME, awsRequest.ruleName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(awsRequest.ruleName(), OPERATION, e);
        }
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                StringUtils.isNullOrEmpty(request.getLogicalResourceIdentifier()) ? "TopicRule" : request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_RULE_NAME_LENGTH).replace("-", "_");
    }

    private void tryGetTopicRule(final String ruleName, final ProxyClient<IotClient> proxyClient) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(GetTopicRuleRequest.builder().ruleName(ruleName).build(), proxyClient.client()::getTopicRule);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, ruleName);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN && !(e instanceof UnauthorizedException || e instanceof ResourceNotFoundException)) {
                throw Translator.translateIotExceptionToHandlerException(ruleName, OPERATION, e);
            }
        }
    }
}
