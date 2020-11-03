package com.amazonaws.iot.topicruledestination;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdateTopicRuleDestinationRequest;
import software.amazon.awssdk.services.iot.model.UpdateTopicRuleDestinationResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class UpdateHandler extends BaseHandlerStd {
    private static final String OPERATION = "UpdateTopicRuleDestination";
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel prevModel = request.getPreviousResourceState() == null ? request.getDesiredResourceState() : request.getPreviousResourceState();
        ResourceModel newModel = request.getDesiredResourceState();

        validatePropertiesAreUpdatable(newModel, prevModel);

        if (StringUtils.isEmpty(newModel.getArn())) {
            throw new CfnNotFoundException(InvalidParameterValueException.builder().message("Parameter 'Arn' must be provided.").build());
        }
        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-IoT-TopicRuleDestination::Update", proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .backoffDelay(DELAY_CONSTANT)
                                .makeServiceCall(this::updateResource)
                                .stabilize(this::stabilizedOnFirstUpdate)
                                .progress())
                // describe call/chain to return the resource model
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdateTopicRuleDestinationResponse updateResource(final UpdateTopicRuleDestinationRequest awsRequest,
                                                              final ProxyClient<IotClient> proxyClient) {
        try {
            UpdateTopicRuleDestinationResponse awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest,
                    proxyClient.client()::updateTopicRuleDestination);
            logger.log(String.format("%s [%s] has successfully been updated.", ResourceModel.TYPE_NAME, awsRequest.arn()));
            return awsResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(awsRequest.arn(), OPERATION, e);
        }
    }

    /**
     * If your resource requires some form of stabilization (e.g. service does not provide strong consistency), you will need to ensure that your code
     * accounts for any potential issues, so that a subsequent read/update requests will not cause any conflicts (e.g. NotFoundException/InvalidRequestException)
     * for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
     * @param awsResponse the aws service  update resource response
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnFirstUpdate(
            final AwsRequest awsRequest,
            final AwsResponse awsResponse,
            final ProxyClient<IotClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        // TODO: put your stabilization code here

        final boolean stabilized = true;

        logger.log(String.format("%s [%s] update has stabilized: %s", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), stabilized));
        return stabilized;
    }

    private void validatePropertiesAreUpdatable(final ResourceModel newModel, final ResourceModel prevModel) {
        if (!StringUtils.equals(newModel.getArn(), prevModel.getArn())) {
            throwCfnNotUpdatableException("Arn");
        } else if (!StringUtils.equals(newModel.getStatusReason(),
                prevModel.getStatusReason())) {
            throwCfnNotUpdatableException("StatusReason"); //only update on status is supported
        } else if (!Objects.equals(newModel.getHttpUrlProperties(), prevModel.getHttpUrlProperties())) {
            throwCfnNotUpdatableException("HttpUrlProperties");
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidParameterValueException.builder()
                .message(String.format("Parameter '%s' is not updatable.", propertyName))
                .build());
    }
}
