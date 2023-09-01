package com.amazonaws.iot.topicrule;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "GetTopicRule";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        return proxy.initiate("AWS-IoT-TopicRule::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .backoffDelay(DELAY_CONSTANT)
                .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient))
                .done((awsRequest, awsResponse, client, resourceModel, context) -> constructResourceModelFromResponse(client, awsResponse));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private GetTopicRuleResponse readResource(final GetTopicRuleRequest awsRequest, final ProxyClient<IotClient> proxyClient) {
        try {
            GetTopicRuleResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::getTopicRule);
            logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, awsRequest.ruleName()));
            return awsResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(awsRequest.ruleName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(final ProxyClient<IotClient> proxyInvocation,
                                                                                             final GetTopicRuleResponse awsResponse) {
        final ResourceModel model = Translator.translateFromReadResponse(awsResponse);
        try {
            List<Tag> tags = listTags(proxyInvocation, awsResponse.ruleArn());
            model.setTags(Translator.translateSdkTagsToResourceTags(tags));
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(awsResponse.rule().ruleName(), OPERATION, e);
            }
        }
        return ProgressEvent.defaultSuccessHandler(model);
    }
}
