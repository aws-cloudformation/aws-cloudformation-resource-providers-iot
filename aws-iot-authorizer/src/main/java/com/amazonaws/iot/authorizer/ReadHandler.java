package com.amazonaws.iot.authorizer;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "DescribeAuthorizer";
    private static final String CALL_GRAPH = "AWS-IoT-Authorizer::Read";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        return proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .backoffDelay(DELAY_CONSTANT)
                .makeServiceCall(this::readResource)
                .done(this::constructResourceModelFromResponse);
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param request     the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private DescribeAuthorizerResponse readResource(final DescribeAuthorizerRequest request, final ProxyClient<IotClient> proxyClient) {
        try {
            DescribeAuthorizerResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::describeAuthorizer);
            logger.log(String.format("%s [%s] has successfully been read.", ResourceModel.TYPE_NAME, request.authorizerName()));
            return response;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.authorizerName());
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param readResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(final DescribeAuthorizerResponse readResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(readResponse));
    }
}
