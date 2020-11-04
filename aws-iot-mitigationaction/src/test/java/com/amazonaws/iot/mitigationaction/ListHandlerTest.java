package com.amazonaws.iot.mitigationaction;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ARN2;
import static com.amazonaws.iot.mitigationaction.TestConstants.CREATION_DATE1;
import static com.amazonaws.iot.mitigationaction.TestConstants.CREATION_DATE2;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_NAME2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ARN;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_ID;
import static com.amazonaws.iot.mitigationaction.TestConstants.ACTION_PARAMS;
import static com.amazonaws.iot.mitigationaction.TestConstants.CLIENT_REQUEST_TOKEN;
import static com.amazonaws.iot.mitigationaction.TestConstants.DESIRED_TAGS;
import static com.amazonaws.iot.mitigationaction.TestConstants.MITIGATION_ACTION_NAME;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.ListMitigationActionsRequest;
import software.amazon.awssdk.services.iot.model.ListMitigationActionsResponse;
import software.amazon.awssdk.services.iot.model.MitigationActionIdentifier;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private ListHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        handler = new ListHandler();
    }

    @Test
    public void handleRequest_HappyCase_VerifyRequestResponse() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        ListMitigationActionsRequest expectedRequest = ListMitigationActionsRequest.builder()
                .nextToken(request.getNextToken())
                .build();
        MitigationActionIdentifier mitigationActionIdentifier1 = MitigationActionIdentifier.builder()
                .actionArn(ACTION_ARN)
                .actionName(MITIGATION_ACTION_NAME)
                .creationDate(CREATION_DATE1)
                .build();

        MitigationActionIdentifier mitigationActionIdentifier2 = MitigationActionIdentifier.builder()
                .actionArn(ACTION_ARN2)
                .actionName(MITIGATION_ACTION_NAME2)
                .creationDate(CREATION_DATE2)
                .build();

        ListMitigationActionsResponse listResponse = ListMitigationActionsResponse.builder()
                .actionIdentifiers(mitigationActionIdentifier1, mitigationActionIdentifier2)
                .nextToken("nextToken2")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest), any()))
                .thenReturn(listResponse);

        ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        assertThat(response.getNextToken()).isEqualTo("nextToken2");
        List<ResourceModel> expectedModels = Arrays.asList(
                ResourceModel.builder()
                        .actionName(MITIGATION_ACTION_NAME)
                        .mitigationActionArn(ACTION_ARN)
                        .creationDate(CREATION_DATE1.toString())
                        .build(),
                ResourceModel.builder()
                        .actionName(MITIGATION_ACTION_NAME2)
                        .mitigationActionArn(ACTION_ARN2)
                        .creationDate(CREATION_DATE2.toString())
                        .build());
        assertThat(response.getResourceModels()).isEqualTo(expectedModels);
    }

    @Test
    public void handleRequest_ThrowUnauthorized_VerifyTranslation() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken("nextToken1")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(UnauthorizedException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnAccessDeniedException.class);
    }
}