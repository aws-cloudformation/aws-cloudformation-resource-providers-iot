package com.amazonaws.iot.provisioningtemplate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeProvisioningTemplateRequest;
import software.amazon.awssdk.services.iot.model.DescribeProvisioningTemplateResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "DescribeProvisioningTemplate";

    private IotClient iotClient;

    public ReadHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public ReadHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    private ProvisioningHook convertProvisioningHook(software.amazon.awssdk.services.iot.model.ProvisioningHook hook) {
        if (hook == null) {
            return null;
        }
        return ProvisioningHook.builder()
                .targetArn(hook.targetArn())
                .payloadVersion(hook.payloadVersion())
                .build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DescribeProvisioningTemplateRequest templateRequest = DescribeProvisioningTemplateRequest.builder()
                .templateName(model.getTemplateName())
                .build();

        try {
            final DescribeProvisioningTemplateResponse response = proxy.injectCredentialsAndInvokeV2(
                    templateRequest,
                    iotClient::describeProvisioningTemplate);

            return ProgressEvent.defaultSuccessHandler(ResourceModel.builder()
                    .templateArn(response.templateArn())
                    .templateName(response.templateName())
                    .description(response.description())
                    .provisioningRoleArn(response.provisioningRoleArn())
                    .templateBody(response.templateBody())
                    .preProvisioningHook(convertProvisioningHook(response.preProvisioningHook()))
                    .enabled(response.enabled())
                    .build());

        } catch (final InternalFailureException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, templateRequest.templateName());
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnGeneralServiceException(OPERATION, e);
        } catch (final UnauthorizedException e) {
            throw new CfnAccessDeniedException(OPERATION, e);
        }
    }
}
