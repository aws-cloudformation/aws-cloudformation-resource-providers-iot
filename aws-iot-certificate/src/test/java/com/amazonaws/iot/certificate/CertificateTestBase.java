package com.amazonaws.iot.certificate;

import org.junit.jupiter.api.Assertions;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

public class CertificateTestBase {
    protected final static String CERT_ID = "certificateId";
    protected final static String CERT_ARN = "arn:aws:iot:us-east-1:1234567890:certificate/certificateId";
    protected final static String CERT_CSR = "CSR";
    protected final static String CERT_PEM = "PEM";
    protected final static String CERT_CA_PEM = "CA_PEM";
    protected final static String CERT_STATUS = "ACTIVE";
    protected final static String REQUEST_TOKEN = "REQUEST_TOKEN";
    protected final static String LOGICAL_ID = "MyCertificate";
    private static final String CERTIFICATE_MODE_DEFAULT = "Default";
    private static final String CERTIFICATE_MODE_SNI_ONLY = "SNI_ONLY";

    protected ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        return ResourceModel.builder()
                .certificateSigningRequest(CERT_CSR)
                .status(CERT_STATUS);

    }

    protected ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model)
                .previousResourceState(model);
    }

    protected void testExceptionThrown(final AmazonWebServicesClientProxy proxy,
                                       final BaseHandler<CallbackContext> handler,
                                       final Logger logger,
                                       final IotException internalException,
                                       final Class<? extends BaseHandlerException> cfnException) {
        final ResourceModel model = defaultModelBuilder().build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        doThrow(internalException).when(proxy).injectCredentialsAndInvokeV2(any(), any());
        Assertions.assertThrows(cfnException, () -> handler.handleRequest(proxy, request, null, logger));
    }
}
