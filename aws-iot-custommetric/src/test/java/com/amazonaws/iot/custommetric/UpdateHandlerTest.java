package com.amazonaws.iot.custommetric;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateCustomMetricRequest;
import software.amazon.awssdk.services.iot.model.UpdateCustomMetricResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_ARN;
import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_NAME;
import static com.amazonaws.iot.custommetric.TestConstants.DISPLAY_NAME;
import static com.amazonaws.iot.custommetric.TestConstants.DISPLAY_NAME2;
import static com.amazonaws.iot.custommetric.TestConstants.METRIC_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {
    protected static final software.amazon.awssdk.services.iot.model.Tag PREVIOUS_SDK_RESOURCE_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("PreviousTagKey")
                    .value("PreviousTagValue")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.Tag DESIRED_SDK_RESOURCE_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("DesiredTagKey")
                    .value("DesiredTagValue")
                    .build();

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @Spy
    private UpdateHandler handler;

    @AfterEach
    public void afterEach() {
        verifyNoMoreInteractions(proxy);
    }

    @Test
    public void handleRequest_BothValueAndTagsAreUpdated_VerifyRequests() {

        ResourceModel previousModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME)
                .build();
        ResourceModel desiredModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME2)
                .build();
        Map<String, String> desiredTags = ImmutableMap.of("DesiredTagKey", "DesiredTagValue");

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(previousModel)
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceState(desiredModel)
                .desiredResourceTags(desiredTags)
                .build();

        doReturn(Collections.singleton(PREVIOUS_SDK_RESOURCE_TAG))
                .when(handler)
                .listTags(proxy, CUSTOM_METRIC_ARN, logger);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(UpdateCustomMetricResponse.builder().metricArn(CUSTOM_METRIC_ARN).build());

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

        desiredModel.setMetricArn(CUSTOM_METRIC_ARN);
        assertThat(response.getResourceModel()).isEqualTo(desiredModel);

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(3)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IotRequest> submittedIotRequests = requestCaptor.getAllValues();

        UpdateCustomMetricRequest submittedUpdateRequest =
                (UpdateCustomMetricRequest) submittedIotRequests.get(0);
        assertThat(submittedUpdateRequest.metricName()).isEqualTo(CUSTOM_METRIC_NAME);
        assertThat(submittedUpdateRequest.displayName()).isEqualTo(DISPLAY_NAME2);

        TagResourceRequest submittedTagRequest = (TagResourceRequest) submittedIotRequests.get(1);
        assertThat(submittedTagRequest.tags()).isEqualTo(Collections.singletonList(DESIRED_SDK_RESOURCE_TAG));
        assertThat(submittedTagRequest.resourceArn()).isEqualTo(CUSTOM_METRIC_ARN);

        UntagResourceRequest submittedUntagRequest = (UntagResourceRequest) submittedIotRequests.get(2);
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(Collections.singletonList("PreviousTagKey"));
        assertThat(submittedUntagRequest.resourceArn()).isEqualTo(CUSTOM_METRIC_ARN);
    }


    @Test
    public void updateTags_SameKeyDifferentValue_OnlyTagCall() {

        software.amazon.awssdk.services.iot.model.Tag previousTag =
                software.amazon.awssdk.services.iot.model.Tag.builder()
                        .key("DesiredTagKey")
                        .value("PreviousTagValue")
                        .build();
        Map<String, String> desiredTags = ImmutableMap.of("DesiredTagKey", "DesiredTagValue");

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(desiredTags)
                .build();

        doReturn(Collections.singleton(previousTag))
                .when(handler)
                .listTags(proxy, CUSTOM_METRIC_ARN, logger);

        handler.updateTags(proxy, request, CUSTOM_METRIC_ARN, logger);

        ArgumentCaptor<TagResourceRequest> requestCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        TagResourceRequest submittedTagRequest = requestCaptor.getValue();
        assertThat(submittedTagRequest.tags()).isEqualTo(Collections.singletonList(DESIRED_SDK_RESOURCE_TAG));
    }


    @Test
    public void updateTags_NoDesiredTags_OnlyUntagCall() {

        Map<String, String> desiredTags = Collections.emptyMap();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(desiredTags)
                .build();

        doReturn(Collections.singleton(PREVIOUS_SDK_RESOURCE_TAG))
                .when(handler)
                .listTags(proxy, CUSTOM_METRIC_ARN, logger);

        handler.updateTags(proxy, request, CUSTOM_METRIC_ARN, logger);

        ArgumentCaptor<UntagResourceRequest> requestCaptor = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        UntagResourceRequest submittedUntagRequest = requestCaptor.getValue();
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(Collections.singletonList("PreviousTagKey"));
    }

    @Test
    public void handleRequest_UpdateThrowsInvalidRequest_VerifyTranslation() {

        ResourceModel desiredModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME2)
                .build();

        ResourceModel previousModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void updateTags_ApiThrowsException_BubbleUp() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(ImmutableMap.of("DesiredTagKey", "DesiredTagValue"))
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        assertThatThrownBy(() ->
                handler.updateTags(proxy, request, CUSTOM_METRIC_ARN, logger))
                .isInstanceOf(InvalidRequestException.class);
    }
    @Test
    void handleRequest_ResourceAlreadyDeleted_VerifyException() {

        ResourceModel desiredModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME2)
                .build();

        ResourceModel previousModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        // If the resource is already deleted, the update API throws ResourceNotFoundException. Mocking that here.
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                handler.handleRequest(proxy, request, null, logger);
        assertThat(progressEvent.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    void handleRequest_DesiredArnIsPopulatedAndSame_ReturnFailed() {

        ResourceModel desiredModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME2)
                .build();
        ResourceModel previousModel = ResourceModel.builder()
                .metricName(CUSTOM_METRIC_NAME)
                .metricArn(CUSTOM_METRIC_ARN)
                .metricType(METRIC_TYPE)
                .displayName(DISPLAY_NAME2)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(previousModel)
                .desiredResourceState(desiredModel)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> result =
                handler.handleRequest(proxy, request, null, logger);

        assertThat(result).isEqualTo(ProgressEvent.failed(
                desiredModel, null, HandlerErrorCode.InvalidRequest, "MetricArn cannot be updated."));
    }
    // TODO: test system tags when the src code is ready
}
