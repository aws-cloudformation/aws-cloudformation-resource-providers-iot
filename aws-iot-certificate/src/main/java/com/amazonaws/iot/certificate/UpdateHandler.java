package com.amazonaws.iot.certificate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateStateException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest;
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

public class UpdateHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "UpdateCertificate";

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

        // Only the status can be updated
        if (!newModel.getStatus().equals(prevModel.getStatus())) {
            UpdateCertificateRequest updateRequest = UpdateCertificateRequest.builder()
                    .certificateId(prevModel.getId())
                    .newStatus(newModel.getStatus())
                    .build();

            try {
                proxy.injectCredentialsAndInvokeV2(updateRequest, iotClient::updateCertificate);

            } catch (final CertificateStateException | ServiceUnavailableException e) {
                throw new CfnGeneralServiceException(OPERATION, e);
            } catch (final InternalFailureException e) {
                throw new CfnServiceInternalErrorException(OPERATION, e);
            } catch (final InvalidRequestException e) {
                throw new CfnInvalidRequestException(e.getMessage(), e);
            } catch (final ResourceNotFoundException e) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, updateRequest.certificateId());
            } catch (final ThrottlingException e) {
                throw new CfnThrottlingException(OPERATION, e);
            } catch (final UnauthorizedException e) {
                throw new CfnAccessDeniedException(OPERATION, e);
            }
        }

        return ProgressEvent.defaultSuccessHandler(newModel);
    }
}
