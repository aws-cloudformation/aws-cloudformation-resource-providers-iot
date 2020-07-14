package com.amazonaws.iot.certificate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CertificateStateException;
import software.amazon.awssdk.services.iot.model.CertificateStatus;
import software.amazon.awssdk.services.iot.model.DeleteCertificateRequest;
import software.amazon.awssdk.services.iot.model.DeleteConflictException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandler<CallbackContext> {
    private static final int UPDATE_STATUS_DELAY = 3;
    private static final String UPDATE_OPERATION = "UpdateCertificate";
    private static final String DELETE_OPERATION = "DeleteCertificate";

    private IotClient iotClient;

    public DeleteHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public DeleteHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final String certificateId = model.getId();

        String operation = DELETE_OPERATION;

        // We may need two passes to delete a certificate if it is active, the API blocks that deletion so we will
        // first call update to set the status to inactive, then on the next pass we can delete.
        if (CertificateStatus.ACTIVE.toString().equals(model.getStatus())) {
            operation = UPDATE_OPERATION;
            final UpdateCertificateRequest updateStatusRequest = UpdateCertificateRequest.builder()
                    .certificateId(certificateId)
                    .newStatus(CertificateStatus.INACTIVE)
                    .build();
            proxy.injectCredentialsAndInvokeV2(updateStatusRequest, iotClient::updateCertificate);
            model.setStatus(CertificateStatus.INACTIVE.toString());

            logger.log(String.format("Setting certificate [%s] to INACTIVE prior to deletion", certificateId));

            return ProgressEvent.defaultInProgressHandler(callbackContext, UPDATE_STATUS_DELAY, model);
        }

        final DeleteCertificateRequest deleteRequest = DeleteCertificateRequest.builder()
                .certificateId(certificateId)
                .build();

        try {
            proxy.injectCredentialsAndInvokeV2(deleteRequest, iotClient::deleteCertificate);
            logger.log(String.format("%s [%s] deleted successfully", ResourceModel.TYPE_NAME, certificateId));

        } catch (final CertificateStateException e) {
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
        } catch (final DeleteConflictException e) {
            throw new CfnResourceConflictException(ResourceModel.TYPE_NAME, certificateId, e.getMessage());
        } catch (final InternalFailureException e) {
            throw new CfnInternalFailureException(e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(deleteRequest.toString(), e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnGeneralServiceException(operation, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(operation, e);
        } catch (final UnauthorizedException e) {
            throw new CfnAccessDeniedException(operation, e);
        } catch (final ResourceNotFoundException e) {
            // Don't error on missing resources, just allow the default success to return
        }

        return ProgressEvent.defaultSuccessHandler(model);
    }
}
