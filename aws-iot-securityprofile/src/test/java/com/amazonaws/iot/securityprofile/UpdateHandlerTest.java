package com.amazonaws.iot.securityprofile;

import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_CFN;
import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_V1_LIST;
import static com.amazonaws.iot.securityprofile.TestConstants.ADDITIONAL_METRICS_V1_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.ALERT_TARGET_MAP_CFN;
import static com.amazonaws.iot.securityprofile.TestConstants.ALERT_TARGET_MAP_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.BEHAVIOR_1_CFN_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.BEHAVIOR_1_IOT_LIST;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_ARN;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_DESCRIPTION;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_NAME;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_IOT_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_KEY;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_KEY_LIST;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_STRINGMAP;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_2_IOT_LIST;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_2_STRINGMAP;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARN_1;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARN_1_SET;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARN_2;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARN_2_SET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.amazon.awssdk.services.iot.model.AttachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DetachSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.UpdateSecurityProfileResponse;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleRequest_AllFieldsUpdated_VerifyRequests() {

        ResourceModel previousModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .build();
        ResourceModel desiredModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .behaviors(BEHAVIOR_1_CFN_SET)
                .alertTargets(ALERT_TARGET_MAP_CFN)
                .additionalMetricsToRetain(ADDITIONAL_METRICS_V1_SET)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARN_2_SET)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceState(desiredModel)
                .desiredResourceTags(TAG_2_STRINGMAP)
                .build();

        doReturn(TARGET_ARN_1_SET)
                .when(handler)
                .listTargetsForSecurityProfile(proxy, SECURITY_PROFILE_NAME);
        doReturn(TAG_1_IOT_SET)
                .when(handler)
                .listTags(proxy, SECURITY_PROFILE_ARN);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(UpdateSecurityProfileResponse.builder().securityProfileArn(SECURITY_PROFILE_ARN).build());

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        desiredModel.setSecurityProfileArn(SECURITY_PROFILE_ARN);
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(5)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IotRequest> submittedIotRequests = requestCaptor.getAllValues();

        UpdateSecurityProfileRequest submittedUpdateRequest =
                (UpdateSecurityProfileRequest) submittedIotRequests.get(0);
        assertThat(submittedUpdateRequest.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);
        assertThat(submittedUpdateRequest.securityProfileDescription()).isEqualTo(SECURITY_PROFILE_DESCRIPTION);
        assertThat(submittedUpdateRequest.behaviors()).isEqualTo(BEHAVIOR_1_IOT_LIST);
        assertThat(submittedUpdateRequest.alertTargetsAsStrings()).isEqualTo(ALERT_TARGET_MAP_IOT);
        assertThat(submittedUpdateRequest.additionalMetricsToRetain()).isEqualTo(ADDITIONAL_METRICS_V1_LIST);
        assertThat(submittedUpdateRequest.additionalMetricsToRetainV2()).isEqualTo(ADDITIONAL_METRICS_IOT);
        assertThat(submittedUpdateRequest.deleteBehaviors()).isFalse();
        assertThat(submittedUpdateRequest.deleteAlertTargets()).isFalse();
        assertThat(submittedUpdateRequest.deleteAdditionalMetricsToRetain()).isFalse();

        AttachSecurityProfileRequest submittedAttachRequest =
                (AttachSecurityProfileRequest) submittedIotRequests.get(1);
        assertThat(submittedAttachRequest.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);
        assertThat(submittedAttachRequest.securityProfileTargetArn()).isEqualTo(TARGET_ARN_2);

        DetachSecurityProfileRequest submittedDetachRequest =
                (DetachSecurityProfileRequest) submittedIotRequests.get(2);
        assertThat(submittedDetachRequest.securityProfileName()).isEqualTo(SECURITY_PROFILE_NAME);
        assertThat(submittedDetachRequest.securityProfileTargetArn()).isEqualTo(TARGET_ARN_1);

        TagResourceRequest submittedTagRequest = (TagResourceRequest) submittedIotRequests.get(3);
        assertThat(submittedTagRequest.tags()).isEqualTo(TAG_2_IOT_LIST);
        assertThat(submittedTagRequest.resourceArn()).isEqualTo(SECURITY_PROFILE_ARN);

        UntagResourceRequest submittedUntagRequest = (UntagResourceRequest) submittedIotRequests.get(4);
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(TAG_1_KEY_LIST);
        assertThat(submittedUntagRequest.resourceArn()).isEqualTo(SECURITY_PROFILE_ARN);
    }

    @Test
    public void updateSecurityProfile_NullCollections_VerifyDeleteFlags() {

        UpdateSecurityProfileRequest expectedUpdateRequest = UpdateSecurityProfileRequest.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .deleteBehaviors(true)
                .deleteAlertTargets(true)
                .deleteAdditionalMetricsToRetain(true)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedUpdateRequest), any()))
                .thenReturn(UpdateSecurityProfileResponse.builder().securityProfileArn(SECURITY_PROFILE_ARN).build());

        ResourceModel modelWithNullCollections = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .targetArns(TARGET_ARN_2_SET)
                .build();

        String actualArn = handler.updateSecurityProfile(proxy, modelWithNullCollections, logger);
        assertThat(actualArn).isEqualTo(SECURITY_PROFILE_ARN);
    }

    @Test
    public void updateSecurityProfile_EmptyCollections_VerifyDeleteFlags() {

        UpdateSecurityProfileRequest expectedUpdateRequest = UpdateSecurityProfileRequest.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .deleteBehaviors(true)
                .deleteAlertTargets(true)
                .deleteAdditionalMetricsToRetain(true)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedUpdateRequest), any()))
                .thenReturn(UpdateSecurityProfileResponse.builder().securityProfileArn(SECURITY_PROFILE_ARN).build());

        ResourceModel modelWithEmptyCollections = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .targetArns(TARGET_ARN_2_SET)
                .behaviors(Collections.emptySet())
                .alertTargets(Collections.emptyMap())
                .additionalMetricsToRetain(Collections.emptySet())
                .additionalMetricsToRetainV2(Collections.emptySet())
                .build();

        String actualArn = handler.updateSecurityProfile(proxy, modelWithEmptyCollections, logger);
        assertThat(actualArn).isEqualTo(SECURITY_PROFILE_ARN);
    }

    @Test
    public void updateTargetAttachments_3Before3After_2Attach2Detach1Keep() {

        Set<String> previousTargets = ImmutableSet.of("keepTarget", "detachTarget1", "detachTarget2");
        doReturn(previousTargets)
                .when(handler)
                .listTargetsForSecurityProfile(proxy, SECURITY_PROFILE_NAME);

        Set<String> desiredTargets = ImmutableSet.of("keepTarget", "attachTarget1", "attachTarget2");
        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .targetArns(desiredTargets)
                .build();

        handler.updateTargetAttachments(proxy, model, logger);

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(4)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());

        AttachSecurityProfileRequest actualAttachRequest1 =
                (AttachSecurityProfileRequest) requestCaptor.getAllValues().get(0);
        AttachSecurityProfileRequest actualAttachRequest2 =
                (AttachSecurityProfileRequest) requestCaptor.getAllValues().get(1);
        Set<String> actualAttachTargets = ImmutableSet.of(actualAttachRequest1.securityProfileTargetArn(),
                actualAttachRequest2.securityProfileTargetArn());
        assertThat(actualAttachTargets).containsExactlyInAnyOrder("attachTarget1", "attachTarget2");

        DetachSecurityProfileRequest actualDetachRequest1 =
                (DetachSecurityProfileRequest) requestCaptor.getAllValues().get(2);
        DetachSecurityProfileRequest actualDetachRequest2 =
                (DetachSecurityProfileRequest) requestCaptor.getAllValues().get(3);
        Set<String> actualDetachTargets = ImmutableSet.of(actualDetachRequest1.securityProfileTargetArn(),
                actualDetachRequest2.securityProfileTargetArn());
        assertThat(actualDetachTargets).containsExactlyInAnyOrder("detachTarget1", "detachTarget2");
    }

    @Test
    public void updateTags_SameKeyDifferentValue_OnlyTagCall() {

        Map<String, String> desiredTagsCfn = ImmutableMap.of(TAG_1_KEY, "NewValue");
        List<software.amazon.awssdk.services.iot.model.Tag> desiredTagsIot = ImmutableList.of(
                software.amazon.awssdk.services.iot.model.Tag.builder()
                        .key(TAG_1_KEY)
                        .value("NewValue")
                        .build());

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(desiredTagsCfn)
                .build();

        doReturn(TAG_1_IOT_SET)
                .when(handler)
                .listTags(proxy, SECURITY_PROFILE_ARN);

        handler.updateTags(proxy, request, SECURITY_PROFILE_ARN, logger);

        ArgumentCaptor<TagResourceRequest> requestCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        TagResourceRequest submittedTagRequest = requestCaptor.getValue();
        assertThat(submittedTagRequest.tags()).isEqualTo(desiredTagsIot);
    }

    @Test
    public void updateTags_NoDesiredTags_OnlyUntagCall() {

        Map<String, String> desiredTags = Collections.emptyMap();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(desiredTags)
                .build();

        doReturn(TAG_1_IOT_SET)
                .when(handler)
                .listTags(proxy, SECURITY_PROFILE_ARN);

        handler.updateTags(proxy, request, SECURITY_PROFILE_ARN, logger);

        ArgumentCaptor<UntagResourceRequest> requestCaptor = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        UntagResourceRequest submittedUntagRequest = requestCaptor.getValue();
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(TAG_1_KEY_LIST);
    }

    @Test
    public void updateSecurityProfile_ResourceNotFound_VerifyTranslation() {

        ResourceModel desiredModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileDescription(SECURITY_PROFILE_DESCRIPTION)
                .additionalMetricsToRetainV2(ADDITIONAL_METRICS_CFN)
                .targetArns(TARGET_ARN_2_SET)
                .build();
        ResourceModel previousModel = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnNotFoundException.class);
    }

    @Test
    public void updateTags_ApiThrowsException_VerifyTranslation() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(TAG_1_STRINGMAP)
                .desiredResourceTags(TAG_1_STRINGMAP)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        assertThatThrownBy(() ->
                handler.updateTags(proxy, request, SECURITY_PROFILE_ARN, logger))
                .isInstanceOf(CfnInvalidRequestException.class);
    }

    @Test
    public void updateTargetAttachments_AttachThrowsException_VerifyTranslation() {

        Set<String> previousTargets = ImmutableSet.of("keepTarget", "detachTarget1", "detachTarget2");
        doReturn(previousTargets)
                .when(handler)
                .listTargetsForSecurityProfile(proxy, SECURITY_PROFILE_NAME);

        Set<String> desiredTargets = ImmutableSet.of("keepTarget", "attachTarget1", "attachTarget2");
        ResourceModel model = ResourceModel.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .targetArns(desiredTargets)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InternalFailureException.builder().build());

        assertThatThrownBy(() ->
                handler.updateTargetAttachments(proxy, model, logger))
                .isInstanceOf(CfnInternalFailureException.class);
    }

    @Test
    void handleRequest_DesiredArnIsPopulatedAndSame_ReturnFailed() {

        ResourceModel desiredModel = ResourceModel.builder()
                .securityProfileArn(SECURITY_PROFILE_ARN)
                .build();
        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(desiredModel)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> result =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(result).isEqualTo(ProgressEvent.failed(
                desiredModel, null, HandlerErrorCode.InvalidRequest, "Arn cannot be updated."));
    }

    // TODO: test system tags when the src code is ready
}
