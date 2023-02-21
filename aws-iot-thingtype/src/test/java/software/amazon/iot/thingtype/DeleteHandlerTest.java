package software.amazon.iot.thingtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeleteThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

    final DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenReturn(DeleteThingTypeResponse.builder().build());

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
    public void handleRequest_Delete_InvalidRequestException_SoonAfterDeprecate() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.builder()
                        .message("5 minutes after deprecation and then retry")
                        .build())
                .thenReturn(DeleteThingTypeResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, times(2)).deleteThingType(any(DeleteThingTypeRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Deprecate_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, never()).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, never()).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, never()).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, never()).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, never()).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient, never()).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Delete_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Delete_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Delete_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Delete_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Delete_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient).deleteThingType(any(DeleteThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Delete_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.deleteThingType(any(DeleteThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
        verify(iotClient).deleteThingType(any(DeleteThingTypeRequest.class));
    }
}
