package software.amazon.iot.thinggroup;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.OperationStatus;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.TagResourceResponse;
import software.amazon.awssdk.services.iot.model.ThingGroupProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceResponse;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupResponse;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()
                .getThingGroupProperties()
                .getThingGroupDescription())
                .isEqualTo("New description");
    }

    @Test
    public void handleRequest_AddTags() {
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
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName("name")
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
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
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
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
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RemoveAllTags() {
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
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName("name")
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
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
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(apiResponseTags)
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
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags() {
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
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("newKey1", "v1");
        tagMap.put("newKey2", "v2");
        tagMap.put("newKey3", "v3");
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
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
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(apiResponseTags)
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
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags_UpdateDescription() {
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
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("newKey1", "v1");
        tagMap.put("newKey2", "v2");
        tagMap.put("newKey3", "v3");
        final ResourceModel newModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
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
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription(TG_DESCRIPTION)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(apiResponseTags)
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
        Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_failsServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_failsThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_failsUnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build());
        when(iotClient.updateThingGroup(any(UpdateThingGroupRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }
}
