package com.amazonaws.iot.certificate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateCertificateFromCsrRequest;
import software.amazon.awssdk.services.iot.model.CreateCertificateFromCsrResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.RegisterCertificateRequest;
import software.amazon.awssdk.services.iot.model.RegisterCertificateResponse;
import software.amazon.awssdk.services.iot.model.RegisterCertificateWithoutCaRequest;
import software.amazon.awssdk.services.iot.model.RegisterCertificateWithoutCaResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private static final String CERTIFICATE_MODE_DEFAULT = "Default";
    private static final String CERTIFICATE_MODE_SNI_ONLY = "SNI_ONLY";
    private static final String SIGNING_OPERATION = "CreateCertificateFromCsr";
    private static final String REGISTER_OPERATION = "RegisterCertificate";
    private static final String REGISTER_WITHOUT_CA_OPERATION = "RegisterCertificateWithoutCa";

    private IotClient iotClient;

    public CreateHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public CreateHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    private boolean isCsrRequest(final ResourceModel model) {
        final boolean requiredPresent = CERTIFICATE_MODE_DEFAULT.equalsIgnoreCase(model.getCertificateMode()) &&
                model.getCertificateSigningRequest() != null;

        final boolean invalidModel = requiredPresent && (
                model.getCACertificatePem() != null ||
                model.getCertificatePem() != null);

        if (requiredPresent && invalidModel) {
            throw new CfnGeneralServiceException("CACertificatePem and CertificateSigningRequest are not valid with CertificateSigningRequest");
        } else {
            return requiredPresent;
        }
    }

    private boolean isMuliAccountRequest(final ResourceModel model) {
        final boolean requiredPresent = CERTIFICATE_MODE_SNI_ONLY.equalsIgnoreCase(model.getCertificateMode()) &&
                model.getCertificatePem() != null;

        final boolean invalidModel = requiredPresent && (
                model.getCACertificatePem() != null ||
                model.getCertificateSigningRequest() != null);

        if (requiredPresent && invalidModel) {
            throw new CfnGeneralServiceException("CACertificatePem and CertificateSigningRequest are not valid with CertificateMode \"SNI_ONLY\"");
        } else {
            return requiredPresent;
        }
    }

    private boolean isCertificatePemRequest(final ResourceModel model) {
        final boolean requiredPresent = CERTIFICATE_MODE_DEFAULT.equalsIgnoreCase(model.getCertificateMode()) &&
                model.getCertificatePem() != null &&
                model.getCACertificatePem() != null;

        final boolean invalidModel = requiredPresent && model.getCertificateSigningRequest() != null;

        if (requiredPresent && invalidModel) {
            throw new CfnGeneralServiceException("CertificateSigningRequest is not valid for ");
        } else {
            return requiredPresent;
        }
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        if (model.getCertificateMode() == null) {
            model.setCertificateMode(CERTIFICATE_MODE_DEFAULT);
        }

        // Determine the creation mode we are in based on which request fields are present and create accordingly
        IotRequest currentRequest = null;
        String currentOperation = null;
        try {
            if (isMuliAccountRequest(model)) {
                final RegisterCertificateWithoutCaRequest registerRequest = RegisterCertificateWithoutCaRequest.builder()
                        .certificatePem(model.getCertificatePem())
                        .status(model.getStatus())
                        .build();

                currentOperation = REGISTER_WITHOUT_CA_OPERATION;
                currentRequest = registerRequest;

                final RegisterCertificateWithoutCaResponse registerResponse = proxy.injectCredentialsAndInvokeV2(
                        registerRequest,
                        iotClient::registerCertificateWithoutCA);

                model.setArn(registerResponse.certificateArn());
                model.setId(registerResponse.certificateId());

            } else if (isCsrRequest(model)) {
                final CreateCertificateFromCsrRequest signingRequest = CreateCertificateFromCsrRequest.builder()
                        .certificateSigningRequest(model.getCertificateSigningRequest())
                        .build();

                currentOperation = SIGNING_OPERATION;
                currentRequest = signingRequest;

                final CreateCertificateFromCsrResponse signingResponse = proxy.injectCredentialsAndInvokeV2(
                        signingRequest,
                        iotClient::createCertificateFromCsr);

                model.setArn(signingResponse.certificateArn());
                model.setId(signingResponse.certificateId());

                // Update the status to the desired state
                final UpdateCertificateRequest updateRequest = UpdateCertificateRequest.builder()
                        .certificateId(signingResponse.certificateId())
                        .newStatus(model.getStatus())
                        .build();
                proxy.injectCredentialsAndInvokeV2(updateRequest, iotClient::updateCertificate);

            } else if (isCertificatePemRequest(model)) {
                final RegisterCertificateRequest registerRequest = RegisterCertificateRequest.builder()
                        .certificatePem(model.getCertificatePem())
                        .caCertificatePem(model.getCACertificatePem())
                        .status(model.getStatus())
                        .build();

                currentOperation = REGISTER_OPERATION;
                currentRequest = registerRequest;

                final RegisterCertificateResponse registerResponse = proxy.injectCredentialsAndInvokeV2(
                        registerRequest,
                        iotClient::registerCertificate);

                model.setArn(registerResponse.certificateArn());
                model.setId(registerResponse.certificateId());

            } else {
                // Invalid configuration, throw a CFN exception
                throw new CfnGeneralServiceException("Invalid certificate resource configuration");
            }

            logger.log(String.format("%s [%s] registered successfully", ResourceModel.TYPE_NAME, model.getId()));
            return ProgressEvent.defaultSuccessHandler(model);

        } catch (final ResourceAlreadyExistsException e) {
            throw new CfnAlreadyExistsException(e);
        } catch (final InternalFailureException|InternalException e) {
            throw new CfnServiceInternalErrorException(currentOperation, e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(currentRequest.toString(), e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnGeneralServiceException(currentRequest.toString(), e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(currentOperation, e);
        } catch (final UnauthorizedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        }
    }
}
