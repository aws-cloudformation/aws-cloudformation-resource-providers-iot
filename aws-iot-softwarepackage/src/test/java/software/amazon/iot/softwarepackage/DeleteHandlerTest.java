package software.amazon.iot.softwarepackage;

import software.amazon.awssdk.services.iot.model.DeletePackageRequest;
import software.amazon.awssdk.services.iot.model.DeletePackageResponse;
import software.amazon.awssdk.services.iot.model.DeletePackageVersionRequest;
import software.amazon.awssdk.services.iot.model.DeletePackageVersionResponse;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageResponse;
import software.amazon.awssdk.services.iot.model.GetPackageVersionRequest;
import software.amazon.awssdk.services.iot.model.GetPackageVersionResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.UpdatePackageRequest;
import software.amazon.awssdk.services.iot.model.UpdatePackageResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends HandlerTestBase {

    private DeleteHandler handler = new DeleteHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder().packageName(PKG_NAME).build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("TOKEN")
                .build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenReturn(DeletePackageResponse.builder().build());
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deletePackage(any(DeletePackageRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deletePackage(any(DeletePackageRequest.class));
    }

    @Test
    public void handleRequest_InternalError() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deletePackage(any(DeletePackageRequest.class));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deletePackage(any(DeletePackageRequest.class));
    }

    @Test
    public void handleRequest_ServiceUnavailable() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deletePackage(any(DeletePackageRequest.class));
    }

    @Test
    public void handleRequest_Unauthorized() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenReturn(GetPackageResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenReturn(GetPackageVersionResponse.builder().build())
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.deletePackageVersion(any(DeletePackageVersionRequest.class))).thenReturn(DeletePackageVersionResponse.builder().build());
        when(iotClient.deletePackage(any(DeletePackageRequest.class))).thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deletePackage(any(DeletePackageRequest.class));
    }
}
