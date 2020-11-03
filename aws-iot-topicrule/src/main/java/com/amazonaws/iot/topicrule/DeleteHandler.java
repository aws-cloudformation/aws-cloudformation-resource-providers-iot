package com.amazonaws.iot.topicrule;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.DeleteTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.GetTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeleteTopicRule";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        if (StringUtils.isEmpty(model.getId())) {
            throw new CfnNotFoundException(InvalidParameterValueException.builder().message("Parameter 'Id' must be provided.").build());
        }
        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-IoT-TopicRule::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.SUCCESS)
                                        .build()));
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deleteRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteTopicRuleResponse deleteResource(final DeleteTopicRuleRequest deleteRequest, final ProxyClient<IotClient> proxyClient) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(GetTopicRuleRequest.builder().ruleName(deleteRequest.ruleName()).build(),
                    proxyClient.client()::getTopicRule);
            DeleteTopicRuleResponse response = proxyClient.injectCredentialsAndInvokeV2(deleteRequest,
                    proxyClient.client()::deleteTopicRule);
            logger.log(String.format("%s [%s] successfully deleted.", ResourceModel.TYPE_NAME, deleteRequest.ruleName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deleteRequest.ruleName(), OPERATION, e);
        }
    }

    private boolean stabilizedOnDelete(
            final DeleteTopicRuleRequest awsRequest,
            final DeleteTopicRuleResponse awsResponse,
            final ProxyClient<IotClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::getTopicRule);
            return false;
        } catch (UnauthorizedException e) {
            return true;
        }
    }
}
