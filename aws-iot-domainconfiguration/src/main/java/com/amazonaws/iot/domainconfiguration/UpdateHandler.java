package com.amazonaws.iot.domainconfiguration;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateValidationException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "UpdateDomainConfiguration";

    private IotClient iotClient;

    public UpdateHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public UpdateHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    private boolean areServerCertificatesUnchanged(List<String> newModelCerts, List<String> prevModelCerts) {
        if (newModelCerts.size() != prevModelCerts.size()) return false;
        return newModelCerts.containsAll(prevModelCerts);
    }

    private void validatePropertiesAreUpdatable(ResourceModel prevModel, ResourceModel newModel) {
        if (!StringUtils.equals(newModel.getDomainConfigurationName(), prevModel.getDomainConfigurationName())) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "DomainConfigurationName");
        }
        if (!StringUtils.equals(newModel.getDomainName(), prevModel.getDomainName())) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "DomainName");
        }
        if (!StringUtils.equals(newModel.getServiceType(), prevModel.getServiceType())) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "ServiceType");
        }
        if (!StringUtils.equals(newModel.getValidationCertificateArn(), prevModel.getValidationCertificateArn())) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "ValidationCertificateArn");
        }
        if (!areServerCertificatesUnchanged(newModel.getServerCertificateArns(), prevModel.getServerCertificateArns())) {
            throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, "ValidationCertificateArn");
        }
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        final ProgressEvent<ResourceModel, CallbackContext> readResponse = (new ReadHandler(iotClient))
                .handleRequest(proxy, request, CallbackContext.builder().build(), logger);

        if (callbackContext != null && callbackContext.isCreateOrUpdateInProgress()) {
            logger.log(String.format("%s [%s] updated successfully", ResourceModel.TYPE_NAME, model.getDomainConfigurationName()));
            return readResponse;
        }

        final ResourceModel prevModel = request.getPreviousResourceState();

        validatePropertiesAreUpdatable(prevModel, model);

        // Determine if we need to set the removeAuthorizerConfig flag by comparing the original state with the
        // desired state.
        boolean removeAuthorizerConfig = false;
        if (prevModel.getAuthorizerConfig() != null && model.getAuthorizerConfig() == null) {
            removeAuthorizerConfig = true;
        }

        final UpdateDomainConfigurationRequest domainRequest = UpdateDomainConfigurationRequest.builder()
                .domainConfigurationName(model.getDomainConfigurationName())
                .authorizerConfig(ResourceUtil.getSdkAuthorizerConfig(model))
                .domainConfigurationStatus(model.getDomainConfigurationStatus())
                .removeAuthorizerConfig(removeAuthorizerConfig)
                .build();

        try {
            UpdateDomainConfigurationResponse response = proxy.injectCredentialsAndInvokeV2(domainRequest,
                    iotClient::updateDomainConfiguration);
            logger.log(String.format("%s [%s] updated. Waiting for %d seconds before returning success.",
                    ResourceModel.TYPE_NAME, model.getDomainConfigurationName(),ResourceUtil.DELAY_CONSTANT));

            return ProgressEvent.defaultInProgressHandler(
                    CallbackContext.builder().createOrUpdateInProgress(true).build(),
                    ResourceUtil.DELAY_CONSTANT,
                    ResourceModel.builder().
                            domainConfigurationName(response.domainConfigurationName())
                            .arn(response.domainConfigurationArn())
                            .build());
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getDomainConfigurationName());
        } catch (final InvalidRequestException|CertificateValidationException e) {
            throw new CfnInvalidRequestException(domainRequest.toString(), e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        } catch (final InternalFailureException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        }

    }
}
