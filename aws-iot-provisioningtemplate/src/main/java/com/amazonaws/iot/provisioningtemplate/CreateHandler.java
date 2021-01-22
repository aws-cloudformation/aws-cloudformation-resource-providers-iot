package com.amazonaws.iot.provisioningtemplate;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateProvisioningTemplateRequest;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ProvisioningHook;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
    private static final int MAX_TEMPLATE_NAME = 36;
    private static final String OPERATION = "CreateProvisioningTemplate";

    private IotClient iotClient;

    public CreateHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public CreateHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    /**
     * Return the template name if specified in the model or auto-generate one based on the request and
     * the resource's logical ID.
     *
     * @param model The desired resource state
     * @param request The resource handler request (used to get logical ID and request token)
     * @return template name to use in create request
     */
    private static String getTemplateName(final ResourceModel model,
                                          final ResourceHandlerRequest<ResourceModel> request) {
        return StringUtils.isNullOrEmpty(model.getTemplateName())
            ? IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(),
                    MAX_TEMPLATE_NAME)
            : model.getTemplateName();
    }

    /**
     * Get the pre-provisioning hook associated with the resource model or null if it does not exist.
     * @param model The desired resource state
     * @return A converted ProvisioningHook or null
     */
    private static ProvisioningHook getPreProvisioningHook(final ResourceModel model) {
        final com.amazonaws.iot.provisioningtemplate.ProvisioningHook hook = model.getPreProvisioningHook();
        if (hook == null) {
            return null;
        }

        return ProvisioningHook.builder()
                .payloadVersion(hook.getPayloadVersion())
                .targetArn(hook.getTargetArn())
                .build();
    }

    /**
     * Get the converted tags of the request model or return null if none are present
     * @param model
     * @return A collection of tags or null
     */
    private static Collection<Tag> getTags(ResourceModel model) {
        final List<com.amazonaws.iot.provisioningtemplate.Tag> modelTags = model.getTags();
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
        final String templateName = getTemplateName(model, request);
        final CreateProvisioningTemplateRequest templateRequest = CreateProvisioningTemplateRequest.builder()
                .templateName(templateName)
                .description(model.getDescription())
                .templateBody(model.getTemplateBody())
                .enabled(model.getEnabled())
                .provisioningRoleArn(model.getProvisioningRoleArn())
                .preProvisioningHook(getPreProvisioningHook(model))
                .tags(getTags(model))
                .build();
        model.setTemplateName(templateName);

        try {
            proxy.injectCredentialsAndInvokeV2(templateRequest, iotClient::createProvisioningTemplate);
            logger.log(String.format("%s [%s] created successfully", ResourceModel.TYPE_NAME, templateName));
        } catch (final ResourceAlreadyExistsException e) {
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, templateName);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, e.getMessage());
        } catch (final InternalException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnGeneralServiceException(OPERATION, e);
        } catch (final UnauthorizedException e) {
            throw new CfnAccessDeniedException(OPERATION, e);
        }

        return ProgressEvent.defaultSuccessHandler(model);
    }
}
