package software.amazon.iot.billinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.BillingGroupProperties;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.TagResourceResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceResponse;
import software.amazon.awssdk.services.iot.model.UpdateBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.VersionConflictException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    final UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_UpdateDescription() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .billingGroupProperties(BillingGroupProperties.builder()
                                .billingGroupDescription("Updated description")
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getBillingGroupProperties()
                .getBillingGroupDescription())
                .isEqualTo("Updated description");
    }

    @Test
    public void handleRequest_UpdateArn_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .arn("testArn")
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).updateBillingGroup(any(UpdateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_UpdateBillingGroupName_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName("UpdatedBillingGroupName")
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).updateBillingGroup(any(UpdateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_AddTags() {
        Set<software.amazon.iot.billinggroup.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String,String> tagMap = new HashMap<>();
        tagMap.put("k1","v1");
        tagMap.put("k2","v2");
        tagMap.put("k3","v3");
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RemoveAllTags() {
        Set<software.amazon.iot.billinggroup.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
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
                .billingGroupName(BG_Name)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags() {
        Set<software.amazon.iot.billinggroup.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
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
        Map<String,String> tagMap = new HashMap<>();
        tagMap.put("newKey1","v1");
        tagMap.put("newKey2","v2");
        tagMap.put("newKey3","v3");
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
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
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags_UpdateDescription() {
        Set<software.amazon.iot.billinggroup.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.billinggroup.Tag.builder()
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
        Map<String,String> tagMap = new HashMap<>();
        tagMap.put("newKey1","v1");
        tagMap.put("newKey2","v2");
        tagMap.put("newKey3","v3");
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .billingGroupProperties(BillingGroupProperties.builder()
                                .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
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
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Update_InternalFailureException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_InvalidRequestException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_ResourceNotFoundException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_ThrottlingException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_VersionConflictException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(VersionConflictException.builder().build());

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTagsForResource_InternalFailureException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTagsForResource_InvalidRequestException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTagsForResource_ResourceNotFoundException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTagsForResource_ThrottlingException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_InternalFailureException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_InvalidRequestException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_ResourceNotFoundException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_ThrottlingException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InternalFailureException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InvalidRequestException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_LimitExceededException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(LimitExceededException.builder().build());

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ResourceNotFoundException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ThrottlingException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }
}
