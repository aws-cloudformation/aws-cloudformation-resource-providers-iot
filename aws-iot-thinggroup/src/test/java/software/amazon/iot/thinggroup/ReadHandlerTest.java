package software.amazon.iot.thinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThingGroupMetadata;
import software.amazon.awssdk.services.iot.model.ThingGroupProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    ReadHandler handler= new ReadHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .parentGroupName(TG_PARENT_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ResourceModel expectedModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .id(TG_ID)
                .arn(TG_ARN)
                .parentGroupName(TG_PARENT_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .attributePayload(AttributePayload.builder()
                                .attributes(new HashMap<>())
                                .build())
                        .build())
                .tags(new HashSet<>())
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .thingGroupMetadata(ThingGroupMetadata.builder()
                                .parentGroupName(TG_PARENT_NAME)
                                .build())
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription(TG_DESCRIPTION)
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
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_SimpleSuccess_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ResourceModel expectedModel = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .id(TG_ID)
                .arn(TG_ARN)
                .queryString(DG_QUERYSTRING)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .attributePayload(AttributePayload.builder()
                                .attributes(new HashMap<>())
                                .build())
                        .build())
                .tags(new HashSet<>())
                .build();

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
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_ListTags_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

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
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

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
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

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
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

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
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }
}
