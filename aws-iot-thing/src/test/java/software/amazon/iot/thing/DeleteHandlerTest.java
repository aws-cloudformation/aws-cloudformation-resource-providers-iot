package software.amazon.iot.thing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.VersionConflictException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenReturn(DeleteThingResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Delete_VersionConflictException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenThrow(VersionConflictException.builder().build());

        assertThrows(CfnResourceConflictException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
        verify(iotClient).deleteThing(any(DeleteThingRequest.class));
    }

    @Test
    public void handleRequest_Delete_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
        verify(iotClient).deleteThing(any(DeleteThingRequest.class));
    }


    @Test
    public void handleRequest_Delete_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
        verify(iotClient).deleteThing(any(DeleteThingRequest.class));
    }

    @Test
    public void handleRequest_Delete_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
        verify(iotClient).deleteThing(any(DeleteThingRequest.class));
    }

    @Test
    public void handleRequest_Delete_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
        verify(iotClient).deleteThing(any(DeleteThingRequest.class));
    }

    @Test
    public void handleRequest_Delete_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().build()).thenThrow(ResourceNotFoundException.class);
        when(iotClient.deleteThing(any(DeleteThingRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
        verify(iotClient).deleteThing(any(DeleteThingRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
    }

    @Test
    public void handleRequest_Describe_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThing(any(DescribeThingRequest.class));
    }
}
