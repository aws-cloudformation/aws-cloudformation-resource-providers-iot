package com.amazonaws.iot.certificate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.Certificate;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListCertificatesRequest;
import software.amazon.awssdk.services.iot.model.ListCertificatesResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends CertificateTestBase {
    private ListHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Captor
    ArgumentCaptor<ListCertificatesRequest> certificateRequestCaptor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(proxy.injectCredentialsAndInvokeV2(any(ListCertificatesRequest.class), any()))
                .thenReturn(ListCertificatesResponse.builder()
                .nextMarker(null)
                .certificates(Certificate.builder()
                        .certificateId(CERT_ID)
                        .status(CERT_STATUS)
                        .certificateArn(CERT_ARN)
                        .build())
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
            handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        List<ResourceModel> models = response.getResourceModels();
        assertThat(models.size()).isEqualTo(1);

        ResourceModel model = models.get(0);
        assertThat(model.getId()).isEqualTo(CERT_ID);
        assertThat(model.getStatus()).isEqualTo(CERT_STATUS);
    }

    @Test
    public void handleRequest_PassedNextToken() {
        final String nextToken = "NEXT";
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null)
                .nextToken(nextToken)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).thenReturn(ListCertificatesResponse.builder()
                .nextMarker(null)
                .certificates(Certificate.builder()
                        .certificateId(CERT_ID)
                        .status(CERT_STATUS)
                        .certificateArn(CERT_ARN)
                        .build())
                .build());

        handler.handleRequest(proxy, request, null, logger);

        verify(proxy).injectCredentialsAndInvokeV2(certificateRequestCaptor.capture(), any());
        assertThat(certificateRequestCaptor.getValue().marker()).isEqualTo(nextToken);
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        doThrow(InternalFailureException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnServiceInternalErrorException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        doThrow(InvalidRequestException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnInvalidRequestException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }

    @Test
    public void handleRequest_ThrottlingExceptio() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        doThrow(ThrottlingException.builder().build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(), any());

        Assertions.assertThrows(CfnThrottlingException.class, () -> {
            handler.handleRequest(proxy, request, null, logger);
        });
    }
}
