package com.amazonaws.iot.securityprofile;

import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_CFN;
import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_V1_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.ALERT_TARGET_MAP_CFN;
import static com.amazonaws.iot.securityprofile.TestConstants.ALERT_TARGET_MAP_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.BEHAVIOR_1_CFN_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.BEHAVIOR_1_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_ARN;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_DESCRIPTION;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_NAME;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_CFN_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_IOT_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARN_1_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private ReadHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleRequest_AllFieldsPopulated_VerifyRequestResponse() {

        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        DescribeSecurityProfileRequest expectedDescribeRequest = DescribeSecurityProfileRequest.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();
        DescribeSecurityProfileResponse describeResponse = DescribeSecurityProfileResponse.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .behaviors(BEHAVIOR_1_IOT)
                .alertTargetsWithStrings(ALERT_TARGET_MAP_IOT)
                .additionalMetricsToRetain(ADDITIONAL_METRICS_V1_SET)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_IOT)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedDescribeRequest), any()))
                .thenReturn(describeResponse);

        doReturn(TARGET_ARN_1_SET)
                .when(handler)
                .listTargetsForSecurityProfile(proxy, SECURITY_PROFILE_NAME);

        doReturn(TAG_1_IOT_SET)
                .when(handler)
                .listTags(proxy, SECURITY_PROFILE_ARN);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel expectedModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .behaviors(BEHAVIOR_1_CFN_SET)
                .alertTargets(ALERT_TARGET_MAP_CFN)
                .additionalMetricsToRetain(ADDITIONAL_METRICS_V1_SET)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARN_1_SET)
                .tags(TAG_1_CFN_SET)
                .build();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void buildResourceModel_NullCollections_StayNull() {

        Set<software.amazon.awssdk.services.iot.model.Behavior> behaviors = null;
        Map<String, software.amazon.awssdk.services.iot.model.AlertTarget> alertTargetMap = null;
        List<String> additionalMetricsV1 = null;
        List<software.amazon.awssdk.services.iot.model.MetricToRetain> additionalMetricsV2 = null;

        DescribeSecurityProfileResponse describeResponse = DescribeSecurityProfileResponse.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .behaviors(behaviors)
                .alertTargetsWithStrings(alertTargetMap)
                .additionalMetricsToRetain(additionalMetricsV1)
                .additionalMetricsToRetainV2(additionalMetricsV2)
                .build();

        ResourceModel actualModel = handler.buildResourceModel(describeResponse, TARGET_ARN_1_SET, TAG_1_IOT_SET);

        ResourceModel expectedModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .behaviors(null)
                .alertTargets(null)
                .additionalMetricsToRetain(null)
                .additionalMetricsToRetainV2(null)
                .targetArns(TARGET_ARN_1_SET)
                .tags(TAG_1_CFN_SET)
                .build();
        assertThat(actualModel).isEqualTo(expectedModel);
    }

    @Test
    public void handleRequest_DescribeThrowsException_VerifyTranslation() {

        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ThrottlingException.builder().build());

        assertThatThrownBy(() -> handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnThrottlingException.class);
    }
}
