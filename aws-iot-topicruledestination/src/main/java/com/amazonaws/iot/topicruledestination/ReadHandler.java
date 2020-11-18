package com.amazonaws.iot.topicruledestination;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "GetTopicRuleDestination";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        return proxy.initiate("AWS-IoT-TopicRuleDestination::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .backoffDelay(DELAY_CONSTANT)
                .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient))
                .done(this::constructResourceModelFromResponse);
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private GetTopicRuleDestinationResponse readResource(final GetTopicRuleDestinationRequest awsRequest, final ProxyClient<IotClient> proxyClient) {
        try {
            GetTopicRuleDestinationResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest,
                    proxyClient.client()::getTopicRuleDestination);
            logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, awsRequest.arn()));
            return awsResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(awsRequest.arn(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(final GetTopicRuleDestinationResponse awsResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse));
    }
}
