package software.amazon.iot.thinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.OperationStatus;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidQueryException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.TagResourceResponse;
import software.amazon.awssdk.services.iot.model.ThingGroupProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceResponse;
import software.amazon.awssdk.services.iot.model.UpdateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateDynamicThingGroupResponse;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupResponse;
import software.amazon.awssdk.services.iot.model.VersionConflictException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    final UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_UpdateDescription() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("New description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder()
                        .build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription("New description")
                                .build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()
                .getThingGroupProperties()
                .getThingGroupDescription())
                .isEqualTo("New description");
    }

    @Test
    public void handleRequest_UpdateArn_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .arn("testArn")
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_UpdateThingGroupName_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName("updated" + TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_AddTags() {
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Collections.emptySet();
        Set<software.amazon.iot.thinggroup.Tag> tags = new HashSet<>();
        tags.add(software.amazon.iot.thinggroup.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("k1", "v1");
        tagMap.put("k2", "v2");
        tagMap.put("k3", "v3");
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder()
                        .build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ArgumentCaptor<TagResourceRequest> tagRequestCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
        verify(iotClient).tagResource(tagRequestCaptor.capture());
        assertThat(tagRequestCaptor.getValue().tags().size()).isEqualTo(3);
        assertThat(tagRequestCaptor.getValue().tags()).contains(
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k1").value("v1").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k2").value("v2").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k3").value("v3").build()
        );
    }

    @Test
    public void handleRequest_AddStackLevelTags() {
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Collections.emptySet();
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .tags(newTagSet)
                .build();

        Map<String, String> stackTags = new HashMap<>();
        stackTags.put("stackKey1", "stackValue1");
        stackTags.put("stackKey2", "stackValue2");

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .desiredResourceTags(stackTags)
                .build();

        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder()
                        .build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ArgumentCaptor<TagResourceRequest> tagRequestCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
        verify(iotClient).tagResource(tagRequestCaptor.capture());
        assertThat(tagRequestCaptor.getValue().tags().size()).isEqualTo(4); // 2 resource tags + 2 stack tags
        assertThat(tagRequestCaptor.getValue().tags()).contains(
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k1").value("v1").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k2").value("v2").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("stackKey1").value("stackValue1").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("stackKey2").value("stackValue2").build()
        );
    }

    @Test
    public void handleRequest_RemoveAllTags() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Collections.emptySet();
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();

        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder()
                        .build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();

        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder()
                        .build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags_UpdateDescription() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("Updated" + TG_DESCRIPTION)
                        .build())
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();

        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder()
                        .build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription(TG_DESCRIPTION)
                                .build())
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Update_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_VersionConflictException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(VersionConflictException.builder().build());

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_LimitExceededException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(LimitExceededException.builder().build());

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UnTagResource_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UnTagResource_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UnTagResource_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UnTagResource_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenReturn(UpdateThingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingGroup(any(UpdateThingGroupRequest.class));
        verify(iotClient, times(2)).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UpdateDescription_DynamicThingGroup() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("New description")
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenReturn(UpdateDynamicThingGroupResponse.builder().build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription("New description")
                                .build())
                        .queryString(DG_QUERYSTRING)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()
                .getThingGroupProperties()
                .getThingGroupDescription())
                .isEqualTo("New description");
    }

    @Test
    public void handleRequest_AddTags_DynamicThingGroup() {
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Collections.emptySet();
        Set<software.amazon.iot.thinggroup.Tag> tags = new HashSet<>();
        tags.add(software.amazon.iot.thinggroup.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("k1", "v1");
        tagMap.put("k2", "v2");
        tagMap.put("k3", "v3");
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .tags(tags)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenReturn(UpdateDynamicThingGroupResponse.builder().build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ArgumentCaptor<TagResourceRequest> tagRequestCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
        verify(iotClient).tagResource(tagRequestCaptor.capture());
        assertThat(tagRequestCaptor.getValue().tags().size()).isEqualTo(3);
        assertThat(tagRequestCaptor.getValue().tags()).contains(
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k1").value("v1").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k2").value("v2").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("k3").value("v3").build()
        );
    }

    @Test
    public void handleRequest_RemoveAllTags_DynamicThingGroup() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Collections.emptySet();
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();

        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenReturn(UpdateDynamicThingGroupResponse.builder().build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags_DynamicThingGroup() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();

        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenReturn(UpdateDynamicThingGroupResponse.builder().build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags_UpdateDescription_DynamicThingGroup() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("Updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();

        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenReturn(UpdateDynamicThingGroupResponse.builder().build());
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .queryString(DG_QUERYSTRING)
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription(TG_DESCRIPTION)
                                .build())
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_failsOnConvert_DynamicToStatic() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_failsOnConvert_StaticToDynamic() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Dynamic_Update_InternalFailureException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Dynamic_Update_InvalidQueryException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenThrow(InvalidQueryException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Dynamic_Update_InvalidRequestException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Dynamic_Update_ResourceNotFoundException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Dynamic_Update_ThrottlingException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Dynamic_Update_VersionConflictException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class)))
                .thenThrow(VersionConflictException.builder().build());

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateDynamicThingGroup(any(UpdateDynamicThingGroupRequest.class));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingUpdateResourceExceptions() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .build();

        return defaultRequestBuilder(prevModel, newModel).build();
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingDescribeResourceExceptions() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .build();

        return defaultRequestBuilder(prevModel, newModel).build();
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingUntagResourceExceptions() {
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Collections.singleton(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build()
        );
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .tags(newTagSet)
                .build();

        return defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.thinggroup.Tag::getKey,
                                software.amazon.iot.thinggroup.Tag::getValue
                        )
                ))
                .build();
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingTagResourceExceptions() {
        Set<software.amazon.iot.thinggroup.Tag> prevTagSet = Collections.singleton(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build()
        );
        Set<software.amazon.iot.thinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.thinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.thinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        final ResourceModel prevModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription("updated" + TG_DESCRIPTION)
                        .build())
                .tags(newTagSet)
                .build();

        return defaultRequestBuilder(prevModel, newModel).build();
    }
}
