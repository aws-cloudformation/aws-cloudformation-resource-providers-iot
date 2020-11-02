package com.amazonaws.iot.dimension;

import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_ARN;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_NAME;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_TYPE;
import static com.amazonaws.iot.dimension.TestConstants.DIMENSION_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UpdateDimensionRequest;
import software.amazon.awssdk.services.iot.model.UpdateDimensionResponse;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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

    @Test
    public void handleRequest_BothValueAndTagsAreUpdated_VerifyRequests() {

        ResourceModel previousModel = ResourceModel.builder()
                .name(DIMENSION_NAME)
                .type(DIMENSION_TYPE)
                .stringValues(Collections.singletonList("previousValue"))
                .build();
        ResourceModel desiredModel = ResourceModel.builder()
                .name(DIMENSION_NAME)
                .type(DIMENSION_TYPE)
                .stringValues(DIMENSION_VALUE)
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
                .listTags(proxy, DIMENSION_ARN, logger);

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenReturn(UpdateDimensionResponse.builder().arn(DIMENSION_ARN).build());

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

        ArgumentCaptor<IotRequest> requestCaptor = ArgumentCaptor.forClass(IotRequest.class);
        verify(proxy, times(3)).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        List<IotRequest> submittedIotRequests = requestCaptor.getAllValues();

        UpdateDimensionRequest submittedUpdateRequest = (UpdateDimensionRequest) submittedIotRequests.get(0);
        assertThat(submittedUpdateRequest.name()).isEqualTo(DIMENSION_NAME);
        assertThat(submittedUpdateRequest.stringValues()).isEqualTo(DIMENSION_VALUE);

        TagResourceRequest submittedTagRequest = (TagResourceRequest) submittedIotRequests.get(1);
        assertThat(submittedTagRequest.tags()).isEqualTo(Collections.singletonList(DESIRED_SDK_RESOURCE_TAG));
        assertThat(submittedTagRequest.resourceArn()).isEqualTo(DIMENSION_ARN);

        UntagResourceRequest submittedUntagRequest = (UntagResourceRequest) submittedIotRequests.get(2);
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(Collections.singletonList("PreviousTagKey"));
        assertThat(submittedUntagRequest.resourceArn()).isEqualTo(DIMENSION_ARN);
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
                .listTags(proxy, DIMENSION_ARN, logger);

        handler.updateTags(proxy, request, DIMENSION_ARN, logger);

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
                .listTags(proxy, DIMENSION_ARN, logger);

        handler.updateTags(proxy, request, DIMENSION_ARN, logger);

        ArgumentCaptor<UntagResourceRequest> requestCaptor = ArgumentCaptor.forClass(UntagResourceRequest.class);
        verify(proxy).injectCredentialsAndInvokeV2(requestCaptor.capture(), any());
        UntagResourceRequest submittedUntagRequest = requestCaptor.getValue();
        assertThat(submittedUntagRequest.tagKeys()).isEqualTo(Collections.singletonList("PreviousTagKey"));
    }

    @Test
    public void handleRequest_UpdateThrowsIRE_VerifyTranslation() {

        ResourceModel desiredModel = ResourceModel.builder()
                .name(DIMENSION_NAME)
                .type(DIMENSION_TYPE)
                .stringValues(DIMENSION_VALUE)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(ResourceModel.builder().build())
                .desiredResourceState(desiredModel)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InvalidRequestException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnInvalidRequestException.class);
    }

    @Test
    public void updateTags_ApiThrowsIFE_VerifyTranslation() {

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceState(ResourceModel.builder().build())
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .desiredResourceTags(ImmutableMap.of("DesiredTagKey", "DesiredTagValue"))
                .build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(InternalFailureException.builder().build());

        assertThatThrownBy(() ->
                handler.updateTags(proxy, request, DIMENSION_ARN, logger))
                .isInstanceOf(CfnInternalFailureException.class);
    }

    @Test
    void handleRequest_ResourceAlreadyDeleted_VerifyException() {

        ResourceModel desiredModel = ResourceModel.builder()
                .name(DIMENSION_NAME)
                .type(DIMENSION_TYPE)
                .stringValues(DIMENSION_VALUE)
                .build();

        ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .previousResourceTags(ImmutableMap.of("doesn't", "matter"))
                .previousResourceState(ResourceModel.builder().build())
                .desiredResourceState(desiredModel)
                .build();

        // If the resource is already deleted, the update API throws ResourceNotFoundException. Mocking that here.
        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThatThrownBy(() ->
                handler.handleRequest(proxy, request, null, logger))
                .isInstanceOf(CfnNotFoundException.class);
    }

    // TODO: test system tags when the src code is ready
}
