package software.amazon.iot.thinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteDynamicThingGroupResponse;
import software.amazon.awssdk.services.iot.model.DeleteThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingGroupResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.VersionConflictException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    final DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThingGroup(any(DeleteThingGroupRequest.class)))
                .thenReturn(DeleteThingGroupResponse.builder()
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(null);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Delete_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThingGroup(any(DeleteThingGroupRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteThingGroup(any(DeleteThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Delete_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThingGroup(any(DeleteThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteThingGroup(any(DeleteThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Delete_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThingGroup(any(DeleteThingGroupRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteThingGroup(any(DeleteThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Delete_VersionConflictException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThingGroup(any(DeleteThingGroupRequest.class)))
                .thenThrow(VersionConflictException.class);

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteThingGroup(any(DeleteThingGroupRequest.class));
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
    public void handleRequest_SimpleSuccess_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class)))
                .thenReturn(DeleteDynamicThingGroupResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(null);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_DynamicThingGroupDelete_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupDelete_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupDelete_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupDelete_VersionConflictException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder()
                        .queryString(DG_QUERYSTRING)
                        .build());
        when(iotClient.deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class)))
                .thenThrow(VersionConflictException.class);

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).deleteDynamicThingGroup(any(DeleteDynamicThingGroupRequest.class));
    }
}
