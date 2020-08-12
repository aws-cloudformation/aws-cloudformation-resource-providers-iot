package com.amazonaws.iot.authorizer;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.CreateAuthorizerResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandlerStd {
    private static final String OPERATION = "CreateAuthorizer";
    private static final String CALL_GRAPH = "AWS-IoT-Authorizer::Create";
    private static final int MAX_AUTHORIZER_NAME = 128;

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(model.getAuthorizerName())) {
            model.setAuthorizerName(generateName(request));
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::createResource)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param request     the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateAuthorizerResponse createResource(final CreateAuthorizerRequest request, final ProxyClient<IotClient> proxyClient) {
        try {
            CreateAuthorizerResponse response = proxyClient.injectCredentialsAndInvokeV2(request,
                    proxyClient.client()::createAuthorizer);
            logger.log(String.format("%s [%s] successfully created.", ResourceModel.TYPE_NAME, request.authorizerName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.toString(),
                    request.authorizerName());
        }
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                StringUtils.isNullOrEmpty(request.getLogicalResourceIdentifier()) ? "Authorizer" : request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_AUTHORIZER_NAME).replace("-", "_");
    }
}
