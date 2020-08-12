package com.amazonaws.iot.authorizer;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.DeleteAuthorizerResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeleteAuthorizer";
    private static final String CALL_GRAPH = "AWS-IoT-Authorizer::Delete";
    private static final String INACTIVE_STATUS = "INACTIVE";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            ProxyClient<IotClient> proxyClient,
            Logger logger) {
        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isEmpty(model.getAuthorizerName())) {
            throw new CfnNotFoundException(InvalidParameterValueException.builder().message("Parameter 'AuthorizerName' must be provided.").build());
        }

        // Update status so authorizer can be deleted
        if (!model.getStatus().equals(INACTIVE_STATUS)) {
            model.setStatus(INACTIVE_STATUS);
            new UpdateHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.defaultSuccessHandler(null)));
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param request     the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteAuthorizerResponse deleteResource(
            final DeleteAuthorizerRequest request,
            final ProxyClient<IotClient> proxyClient) {
        try {
            DeleteAuthorizerResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    request,
                    proxyClient.client()::deleteAuthorizer);
            logger.log(String.format("%s [%s] successfully deleted.", ResourceModel.TYPE_NAME, request.authorizerName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.toString(),
                    request.authorizerName());
        }
    }

    private boolean stabilizedOnDelete(
            final DeleteAuthorizerRequest awsRequest,
            final DeleteAuthorizerResponse awsResponse,
            final ProxyClient<IotClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeAuthorizer);
            return false;
        } catch (ResourceNotFoundException e) {
            return true;
        }
    }
}
