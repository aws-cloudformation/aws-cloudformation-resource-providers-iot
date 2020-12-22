package com.amazonaws.iot.authorizer;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdateAuthorizerRequest;
import software.amazon.awssdk.services.iot.model.UpdateAuthorizerResponse;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends BaseHandlerStd {
    private static final String OPERATION = "UpdateAuthorizer";
    private static final String CALL_GRAPH = "AWS-IoT-Authorizer::Update";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel prevModel = request.getPreviousResourceState();
        final ResourceModel newModel = request.getDesiredResourceState();

        validatePropertiesAreUpdatable(newModel, prevModel);

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::updateResource)
                                .progress())
                // describe call/chain to return the resource model
                .then(progress -> new ReadHandler().handleRequest(proxy,
                        request.toBuilder().desiredResourceState(newModel).build(),
                        callbackContext,
                        proxyClient,
                        logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param request     the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdateAuthorizerResponse updateResource(final UpdateAuthorizerRequest request,
                                                    final ProxyClient<IotClient> proxyClient) {
        try {
            UpdateAuthorizerResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::updateAuthorizer);
            logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, request.authorizerName()));
            return awsResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    e,
                    OPERATION,
                    request.authorizerName());
        }
    }

    private void validatePropertiesAreUpdatable(final ResourceModel newModel, final ResourceModel prevModel) {
        if (!StringUtils.equals(newModel.getAuthorizerName(), prevModel.getAuthorizerName())) {
            throwCfnNotUpdatableException("AuthorizerName");
        } else if (newModel.getSigningDisabled() != null &&
                !newModel.getSigningDisabled().equals(prevModel.getSigningDisabled())) {
            throwCfnNotUpdatableException("SigningDisabled");
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
