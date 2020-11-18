package com.amazonaws.iot.topicruledestination;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleDestinationResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.TopicRuleDestinationConfiguration;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {
    private static final String OPERATION = "CreateTopicRuleDestination";

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
        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-IoT-TopicRuleDestination::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::createResource)
                                .stabilize(this::stabilizedOnCreate)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateTopicRuleDestinationResponse createResource(final CreateTopicRuleDestinationRequest awsRequest,
                                                              final ProxyClient<IotClient> proxyClient) {
        try {
            CreateTopicRuleDestinationResponse response = proxyClient.injectCredentialsAndInvokeV2(awsRequest,
                    proxyClient.client()::createTopicRuleDestination);
            logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
            return response;
        } catch (IotException e) {
            TopicRuleDestinationConfiguration dest = awsRequest.destinationConfiguration();
            String identifier = dest.httpUrlConfiguration().confirmationUrl();
            throw Translator.translateIotExceptionToHandlerException(identifier, OPERATION, e);
        }
    }

    private boolean stabilizedOnCreate(final CreateTopicRuleDestinationRequest awsRequest,
                                       final CreateTopicRuleDestinationResponse awsResponse,
                                       final ProxyClient<IotClient> proxyClient,
                                       final ResourceModel model,
                                       final CallbackContext callbackContext) {
        model.setArn(awsResponse.topicRuleDestination().arn());
        logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        return true;
    }
}
