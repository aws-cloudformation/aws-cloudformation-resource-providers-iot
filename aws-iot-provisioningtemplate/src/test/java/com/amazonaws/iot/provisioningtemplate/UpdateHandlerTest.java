package com.amazonaws.iot.provisioningtemplate;

import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateProvisioningTemplateVersionRequest;
import software.amazon.awssdk.services.iot.model.CreateProvisioningTemplateVersionResponse;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplateVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplateVersionsResponse;
import software.amazon.awssdk.services.iot.model.ListProvisioningTemplatesRequest;
import software.amazon.awssdk.services.iot.model.ProvisioningTemplateVersionSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // needed to mock same method with different params
public class UpdateHandlerTest extends ProvisioningTemplateTestBase {
    private UpdateHandler handler;

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new UpdateHandler();
    }

    private void mockDefaultVersions() {
        doReturn(ListProvisioningTemplateVersionsResponse.builder()
                .versions(ProvisioningTemplateVersionSummary.builder()
                        .isDefaultVersion(true)
                        .creationDate(Instant.now())
                        .versionId(1)
                        .build())
                .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(ListProvisioningTemplateVersionsRequest.class), any());
    }

    private ProvisioningTemplateVersionSummary.Builder defaultVersionSummaryBuilder() {
        return ProvisioningTemplateVersionSummary.builder()
                .isDefaultVersion(false)
                .creationDate(Instant.now())
                .versionId(1);
    }

    @Test
    public void handleRequest_updatesNonBodyAttributes() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setDescription("new");
        newModel.setEnabled(false);

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        mockDefaultVersions();
        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTemplateName()).isEqualTo(TEMPLATE_NAME);

        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void handleRequest_updatesTemplateBody() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setTemplateBody("{}");

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        mockDefaultVersions();
        doReturn(CreateProvisioningTemplateVersionResponse.builder()
                    .isDefaultVersion(true)
                    .templateName(TEMPLATE_NAME)
                    .templateArn(TEMPLATE_ARN)
                    .versionId(2)
                    .build())
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(CreateProvisioningTemplateVersionRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTemplateName()).isEqualTo(TEMPLATE_NAME);

        verify(proxy, times(2)).injectCredentialsAndInvokeV2(any(), any());
    }

    @Test
    public void handleRequest_deletesOldestVersion() {
        final ResourceModel prevModel = defaultModelBuilder().build();
        final ResourceModel newModel = defaultModelBuilder().build();

        newModel.setTemplateBody("{}");

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(CreateProvisioningTemplateVersionRequest.class), any()))
                .thenThrow(LimitExceededException.builder().build())
                .thenReturn(CreateProvisioningTemplateVersionResponse.builder().build());

        ListProvisioningTemplateVersionsResponse listVersionResponse = ListProvisioningTemplateVersionsResponse.builder()
                .versions(
                        defaultVersionSummaryBuilder().isDefaultVersion(true).build(),
                        defaultVersionSummaryBuilder().build(),
                        defaultVersionSummaryBuilder().build(),
                        defaultVersionSummaryBuilder().build(),
                        defaultVersionSummaryBuilder().build()
                )
                .build();
        doReturn(listVersionResponse)
                .when(proxy)
                .injectCredentialsAndInvokeV2(any(ListProvisioningTemplateVersionsRequest.class), any());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getTemplateName()).isEqualTo(TEMPLATE_NAME);

        verify(proxy, times(6)).injectCredentialsAndInvokeV2(any(), any());
    }

}
