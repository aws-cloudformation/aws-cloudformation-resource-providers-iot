package com.amazonaws.iot.provisioningtemplate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplatesRequest;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplatesResponse;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "ListProvisioningTemplates";

    private IotClient iotClient;

    public ListHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public ListHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ListProvisioningTemplatesRequest templatesRequest = ListProvisioningTemplatesRequest.builder()
                .maxResults(50)
                .nextToken(request.getNextToken())
                .build();

        try {
            final ListProvisioningTemplatesResponse response = proxy.injectCredentialsAndInvokeV2(
                    templatesRequest,
                    iotClient::listProvisioningTemplates);

            final List<ResourceModel> models = response.templates().stream()
                    .map(template -> ResourceModel.builder()
                            .templateName(template.templateName())
                            .description(template.description())
                            .enabled(template.enabled())
                            .templateArn(template.templateArn())
                            .build())
                    .collect(Collectors.toList());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .nextToken(response.nextToken())
                    .status(OperationStatus.SUCCESS)
                    .build();

        } catch (final InternalFailureException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnGeneralServiceException(OPERATION, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        } catch (final UnauthorizedException e) {
            throw new CfnAccessDeniedException(OPERATION, e);
        }
    }
}
