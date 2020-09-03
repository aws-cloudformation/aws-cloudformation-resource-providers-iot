package com.amazonaws.iot.domainconfiguration;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.CreateDomainConfigurationResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationRequest;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private static final int MAX_DOMAIN_CONFIG_NAME = 128;
    private static final String OPERATION = "CreateDomainConfiguration";

    private IotClient iotClient;

    public CreateHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public CreateHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    /**
     * Return the domain configuration name if specified in the model or auto-generate one based on the request and
     * the resource's logical ID.
     *
     * @param model The desired resource state
     * @param request The resource handler request (used to get logical ID and request token)
     * @return template name to use in create request
     */
    private static String getDomainConfigurationName(final ResourceModel model,
                                          final ResourceHandlerRequest<ResourceModel> request) {
        return StringUtils.isNullOrEmpty(model.getDomainConfigurationName())
            ? IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    MAX_DOMAIN_CONFIG_NAME)
            : model.getDomainConfigurationName();
    }

    /**
     * Get the converted tags of the request model or return null if none are present
     * @param model
     * @return A collection of tags or null
     */
    private static Collection<Tag> getTags(ResourceModel model) {
        final List<Tags> modelTags = model.getTags();
        return Objects.isNull(modelTags)
                ? null
                : modelTags.stream()
                    .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                    .collect(Collectors.toList());
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String domainConfigName = getDomainConfigurationName(model, request);
        model.setDomainConfigurationName(domainConfigName);

        if (callbackContext != null && callbackContext.isCreateOrUpdateInProgress()) {
            int currentRetryCount = callbackContext.getRetryCount();
            try {
                final ProgressEvent<ResourceModel, CallbackContext> readResponse = (new ReadHandler(iotClient))
                        .handleRequest(proxy, request, CallbackContext.builder().build(), logger);
                logger.log(String.format("%s [%s] created successfully", ResourceModel.TYPE_NAME, domainConfigName));
                return readResponse;
            } catch (final CfnNotFoundException e) {
                if(currentRetryCount >= ResourceUtil.MAX_RETRIES)
                    throw new CfnResourceConflictException(model.getDomainName(), model.getDomainConfigurationArn(),
                            "Unable to create the resource", e);
                else
                    return ProgressEvent.defaultInProgressHandler(
                            CallbackContext.builder().createOrUpdateInProgress(true).retryCount(currentRetryCount + 1).build(),
                            ResourceUtil.DELAY_CONSTANT, model);
            }
        }

        final CreateDomainConfigurationRequest domainRequest = CreateDomainConfigurationRequest.builder()
                .domainConfigurationName(domainConfigName)
                .domainName(model.getDomainName())
                .authorizerConfig(ResourceUtil.getSdkAuthorizerConfig(model))
                .serverCertificateArns(model.getServerCertificateArns())
                .serviceType(model.getServiceType())
                .tags(getTags(model))
                .validationCertificateArn(model.getValidationCertificateArn())
                .build();

        try {
            CreateDomainConfigurationResponse response = proxy.injectCredentialsAndInvokeV2(domainRequest,
                    iotClient::createDomainConfiguration);
            logger.log(String.format("%s [%s] created. Waiting for %d seconds before returning success",
                    ResourceModel.TYPE_NAME, domainConfigName, ResourceUtil.DELAY_CONSTANT));

            // Since we have a property that only shows up in updates, we need to handle it in create as well as
            // there is no support for updateOnlyProperties.
            if (model.getDomainConfigurationStatus() != null) {
                UpdateDomainConfigurationRequest updateRequest = UpdateDomainConfigurationRequest.builder()
                        .domainConfigurationName(domainConfigName)
                        .domainConfigurationStatus(model.getDomainConfigurationStatus())
                        .build();
                proxy.injectCredentialsAndInvokeV2(updateRequest, iotClient::updateDomainConfiguration);
                logger.log(String.format("%s [%s] updated during creation with status [%s]",
                        ResourceModel.TYPE_NAME,
                        domainConfigName,
                        model.getDomainConfigurationStatus()));

            }
            return ProgressEvent.defaultInProgressHandler(
                    CallbackContext.builder().createOrUpdateInProgress(true).retryCount(1).build(),
                    ResourceUtil.DELAY_CONSTANT,
                    ResourceModel.builder().
                            domainConfigurationName(response.domainConfigurationName())
                            .domainConfigurationArn(response.domainConfigurationArn())
                            .build());

        } catch (final ResourceAlreadyExistsException e) {
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, domainConfigName);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(domainRequest.toString(), e);
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, e.toString());
        } catch (final InternalException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        }
    }
}
