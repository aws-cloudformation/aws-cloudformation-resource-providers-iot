package com.amazonaws.iot.policy;


import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iot.model.GetPolicyResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class CreateHandler extends BaseHandlerStd{
    private static final String OPERATION = "CreatePolicy";
    private static final String CALL_GRAPH = "AWS-IoT-Policy::Create";
    private static final int MAX_POLICY_NAME = 128;
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel model = request.getDesiredResourceState();

        if (StringUtils.isNullOrEmpty(model.getPolicyName())) {
            model.setPolicyName(generateName(request));
        }

        model.setId(model.getPolicyName());

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
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
    private CreatePolicyResponse createResource(final CreatePolicyRequest request, final ProxyClient<IotClient> proxyClient) {
        try {
            CreatePolicyResponse response = proxyClient.injectCredentialsAndInvokeV2(request,
                    proxyClient.client()::createPolicy);
            logger.log(String.format("%s [%s] successfully created.", ResourceModel.TYPE_NAME, request.policyName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.policyName());
        }
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                StringUtils.isNullOrEmpty(request.getLogicalResourceIdentifier()) ? "Policy" : request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_POLICY_NAME).replace("-", "_");
    }




}
