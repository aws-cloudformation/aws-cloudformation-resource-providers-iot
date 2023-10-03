package software.amazon.iot.softwarepackageversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageResponse;
import software.amazon.awssdk.services.iot.model.GetPackageVersionRequest;
import software.amazon.awssdk.services.iot.model.GetPackageVersionResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.PackageVersionStatus;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
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

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestBase {

    ReadHandler handler = new ReadHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        final GetPackageVersionResponse getPackageVersionResponse =
                GetPackageVersionResponse
                        .builder()
                        .packageName(PKG_NAME)
                        .versionName(VER_NAME)
                        .packageVersionArn(PKG_VER_ARN)
                        .build();

        final ListTagsForResourceResponse listTagsForResourceResponse =
                ListTagsForResourceResponse
                        .builder()
                        .tags(Collections.emptyList())
                        .build();

        ResourceModel expectedModel = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .packageVersionArn(PKG_VER_ARN)
                .errorReason("")
                .attributes(Collections.emptyMap())
                .tags(Collections.emptySet())
                .build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class))).thenReturn(getPackageVersionResponse);

        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForResourceResponse);

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
    public void handleRequest_AllPropertiesWithTags() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        final GetPackageVersionResponse getPackageVersionResponse =
                GetPackageVersionResponse
                        .builder()
                        .packageName(PKG_NAME)
                        .versionName(VER_NAME)
                        .packageVersionArn(PKG_VER_ARN)
                        .description(PKG_VER_DESC)
                        .status(PackageVersionStatus.PUBLISHED)
                        .attributes(Collections.singletonMap("key", "value"))
                        .build();

        final ListTagsForResourceResponse listTagsForResourceResponse =
                ListTagsForResourceResponse
                        .builder()
                        .tags(Collections.singletonList(Tag.builder().key("key").value("value").build()))
                        .build();

        ResourceModel expectedModel = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .packageVersionArn(PKG_VER_ARN)
                .description(PKG_VER_DESC)
                .status(PackageVersionStatus.PUBLISHED.toString())
                .errorReason("")
                .attributes(Collections.singletonMap("key", "value"))
                .tags(Collections.singleton(new software.amazon.iot.softwarepackageversion.Tag("key", "value")))
                .build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class))).thenReturn(getPackageVersionResponse);

        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForResourceResponse);

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
    public void handleRequest_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackageVersion(any(GetPackageVersionRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackageVersion(any(GetPackageVersionRequest.class));
    }

    @Test
    public void handleRequest_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackageVersion(any(GetPackageVersionRequest.class));
    }

    @Test
    public void handleRequest_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackageVersion(any(GetPackageVersionRequest.class));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackageVersion(any(GetPackageVersionRequest.class));
    }

    @Test
    public void handleRequest_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .versionName(VER_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackageVersion(any(GetPackageVersionRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackageVersion(any(GetPackageVersionRequest.class));
    }
}
