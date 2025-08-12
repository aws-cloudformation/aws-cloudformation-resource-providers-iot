package software.amazon.iot.billinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.BillingGroupProperties;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
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
        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Collections.emptySet();
        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
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
    public void handleRequest_AddStackLevelTags() {
        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Collections.emptySet();
        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(newTagSet)
                .build();

        Map<String, String> stackTags = new HashMap<>();
        stackTags.put("stackKey1", "stackValue1");
        stackTags.put("stackKey2", "stackValue2");

        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .desiredResourceTags(stackTags)
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
        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Collections.emptySet();
        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.billinggroup.Tag::getKey,
                                software.amazon.iot.billinggroup.Tag::getValue
                        )
                ))
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
        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.billinggroup.Tag::getKey,
                                software.amazon.iot.billinggroup.Tag::getValue
                        )
                ))
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
        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k3").value("v3").build()
        ).collect(Collectors.toSet());
        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());
        final ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .tags(newTagSet)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.billinggroup.Tag::getKey,
                                software.amazon.iot.billinggroup.Tag::getValue
                        )
                ))
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
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Update_VersionConflictException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUpdateResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenThrow(VersionConflictException.builder().build());

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient, never()).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingDescribeResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder()
                        .build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingUntagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_LimitExceededException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(LimitExceededException.builder().build());

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ResourceNotFoundException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = createTestRequestForTestingTagResourceExceptions();

        when(iotClient.updateBillingGroup(any(UpdateBillingGroupRequest.class)))
                .thenReturn(UpdateBillingGroupResponse.builder().build());
        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateBillingGroup(any(UpdateBillingGroupRequest.class));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingUpdateResourceExceptions() {
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
        return defaultRequestBuilder(prevModel, newModel)
                .build();
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingDescribeResourceExceptions() {
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
        return defaultRequestBuilder(prevModel, newModel)
                .build();
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingUntagResourceExceptions() {
        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Collections.singleton(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build()
        );

        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();

        ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .tags(newTagSet)
                .build();

        return defaultRequestBuilder(prevModel, newModel)
                .previousResourceTags(prevTagSet.stream().collect(
                        Collectors.toMap(
                                software.amazon.iot.billinggroup.Tag::getKey,
                                software.amazon.iot.billinggroup.Tag::getValue
                        )
                ))
                .build();
    }

    private ResourceHandlerRequest<ResourceModel> createTestRequestForTestingTagResourceExceptions() {
        Set<software.amazon.iot.billinggroup.Tag> prevTagSet = Collections.singleton(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build()
        );

        Set<software.amazon.iot.billinggroup.Tag> newTagSet = Stream.of(
                software.amazon.iot.billinggroup.Tag.builder().key("k1").value("v1").build(),
                software.amazon.iot.billinggroup.Tag.builder().key("k2").value("v2").build()
        ).collect(Collectors.toSet());

        ResourceModel prevModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .tags(prevTagSet)
                .build();

        ResourceModel newModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription("Updated description")
                        .build())
                .tags(newTagSet)
                .build();

        return defaultRequestBuilder(prevModel, newModel).build();
    }
}
