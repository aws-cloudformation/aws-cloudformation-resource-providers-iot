package com.amazonaws.iot.provisioningtemplate;

import com.amazonaws.AmazonWebServiceRequest;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateProvisioningTemplateVersionRequest;
import software.amazon.awssdk.services.iot.model.DeleteProvisioningTemplateVersionRequest;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplateVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplateVersionsResponse;
import software.amazon.awssdk.services.iot.model.ProvisioningTemplateVersionSummary;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UpdateProvisioningTemplateRequest;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "UpdateProvisioningTemplate";

    private IotClient iotClient;

    public UpdateHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public UpdateHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel newModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();
        final String templateName = newModel.getTemplateName();
        IotRequest currentRequest = null;

        try {
            // Now we can create a new version if we need one, compare to the current default version (the most recent).
            if (!prevModel.getTemplateBody().equals(newModel.getTemplateBody())) {
                CreateProvisioningTemplateVersionRequest createVersionRequest = CreateProvisioningTemplateVersionRequest.builder()
                        .templateName(templateName)
                        .templateBody(newModel.getTemplateBody())
                        .setAsDefault(true)
                        .build();

                // Attempt to create the version first
                try {
                    proxy.injectCredentialsAndInvokeV2(createVersionRequest, iotClient::createProvisioningTemplateVersion);
                } catch (LimitExceededException e) {
                    // Find a version to delete (the oldest)
                    ListProvisioningTemplateVersionsRequest listVersionRequest = ListProvisioningTemplateVersionsRequest.builder()
                            .templateName(templateName)
                            .build();
                    currentRequest = listVersionRequest;
                    final ListProvisioningTemplateVersionsResponse listVersionResult = proxy.injectCredentialsAndInvokeV2(
                            listVersionRequest,
                            iotClient::listProvisioningTemplateVersions);

                    final List<ProvisioningTemplateVersionSummary> versions = listVersionResult.versions().stream()
                            .sorted(Comparator.comparing(ProvisioningTemplateVersionSummary::versionId))
                            .collect(Collectors.toList());

                    // For consistency we always delete the oldest version, so check now if that is the default currently
                    if (versions.get(0).isDefaultVersion()) {
                        UpdateProvisioningTemplateRequest updateRequest = UpdateProvisioningTemplateRequest.builder()
                                .defaultVersionId(versions.get(versions.size() - 1).versionId())
                                .build();
                        currentRequest = updateRequest;
                        proxy.injectCredentialsAndInvokeV2(updateRequest, iotClient::updateProvisioningTemplate);
                    }

                    DeleteProvisioningTemplateVersionRequest deleteVersionRequest = DeleteProvisioningTemplateVersionRequest.builder()
                            .templateName(templateName)
                            .versionId(versions.get(0).versionId())
                            .build();
                    currentRequest = deleteVersionRequest;
                    proxy.injectCredentialsAndInvokeV2(deleteVersionRequest, iotClient::deleteProvisioningTemplateVersion);
                    currentRequest = createVersionRequest;
                    proxy.injectCredentialsAndInvokeV2(createVersionRequest, iotClient::createProvisioningTemplateVersion);
                }
            }

            UpdateProvisioningTemplateRequest updateRequest = UpdateProvisioningTemplateRequest.builder()
                    .templateName(templateName)
                    .description(newModel.getDescription())
                    .enabled(newModel.getEnabled())
                    .provisioningRoleArn(newModel.getProvisioningRoleArn())
                    .build();
            currentRequest = updateRequest;
            proxy.injectCredentialsAndInvokeV2(updateRequest, iotClient::updateProvisioningTemplate);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(currentRequest.toString(), e);
        } catch (final InternalException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        }

        return ProgressEvent.defaultSuccessHandler(newModel);
    }
}
