package software.amazon.iot.softwarepackageversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.OperationStatus;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListPackageVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPackageVersionsResponse;
import software.amazon.awssdk.services.iot.model.ListPackagesRequest;
import software.amazon.awssdk.services.iot.model.ListPackagesResponse;
import software.amazon.awssdk.services.iot.model.PackageSummary;
import software.amazon.awssdk.services.iot.model.PackageVersionSummary;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends HandlerTestBase {

    ListHandler handler = new ListHandler();

//    @Test
//    public void handleRequest_SimpleSuccess() {
//        final ResourceModel model = ResourceModel.builder().build();
//        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();
//
//        when(iotClient.listPackageVersions(any(ListPackageVersionsRequest.class)))
//                .thenReturn(ListPackageVersionsResponse.builder()
//                        .packageVersionSummaries(PackageVersionSummary.builder().packageName(PKG_NAME).versionName(VER_NAME).build())
//                        .build());
//
//        final ProgressEvent<ResourceModel, CallbackContext> response =
//                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient, LOGGER);
//
//        assertThat(response).isNotNull();
//        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
//        assertThat(response.getCallbackContext()).isNull();
//        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
//        assertThat(response.getResourceModel()).isNull();
//        assertThat(response.getResourceModels()).isNotNull();
//        assertThat(response.getMessage()).isNull();
//        assertThat(response.getErrorCode()).isNull();
//    }

    @Test
    public void handleRequest_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listPackageVersions(any(ListPackageVersionsRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listPackageVersions(any(ListPackageVersionsRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listPackageVersions(any(ListPackageVersionsRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listPackageVersions(any(ListPackageVersionsRequest.class));
    }

    @Test
    public void handleRequest_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listPackageVersions(any(ListPackageVersionsRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listPackageVersions(any(ListPackageVersionsRequest.class));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listPackageVersions(any(ListPackageVersionsRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listPackageVersions(any(ListPackageVersionsRequest.class));
    }

    @Test
    public void handleRequest_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listPackageVersions(any(ListPackageVersionsRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listPackageVersions(any(ListPackageVersionsRequest.class));
    }
}
