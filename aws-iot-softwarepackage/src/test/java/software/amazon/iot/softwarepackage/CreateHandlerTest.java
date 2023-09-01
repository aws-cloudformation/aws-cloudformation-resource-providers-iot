package software.amazon.iot.softwarepackage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreatePackageRequest;
import software.amazon.awssdk.services.iot.model.CreatePackageResponse;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends HandlerTestBase {

    CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreatePackageResponse createPackageResponse =
                CreatePackageResponse
                        .builder()
                        .packageName(PKG_NAME)
                        .packageArn(PKG_ARN)
                        .build();
        final GetPackageResponse getPackageResponse =
                GetPackageResponse
                        .builder()
                        .packageName(PKG_NAME)
                        .packageArn(PKG_ARN)
                        .defaultVersionName(DEFAULT_VER_NAME)
                        .build();

        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse.builder().tags(Collections.emptyList()).build();
        final ResourceModel model = ResourceModel.builder()
                .packageName(getPackageResponse.packageName())
                .packageArn(getPackageResponse.packageArn())
                .tags(Collections.emptyMap())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("TOKEN")
                .build();

        when(iotClient.createPackage(any(CreatePackageRequest.class))).thenReturn(createPackageResponse);
        when(iotClient.getPackage(any(GetPackageRequest.class))).thenReturn(getPackageResponse);
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForResourceResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AllPropertiesWithTags() {
        final CreatePackageResponse createPackageResponse =
                CreatePackageResponse
                        .builder()
                        .packageName(PKG_NAME)
                        .packageArn(PKG_ARN)
                        .description(PKG_DESC)
                        .build();

        final GetPackageResponse getPackageResponse =
                GetPackageResponse
                        .builder()
                        .packageName(PKG_NAME)
                        .packageArn(PKG_ARN)
                        .description(PKG_DESC)
                        .defaultVersionName(DEFAULT_VER_NAME)
                        .build();

        final Map<String, String> TAGS = new HashMap<String, String>() {{
            put("key1", "value1");
            put("key2", "value2");
        }};

        final ListTagsForResourceResponse listTagsForResourceResponse =
                ListTagsForResourceResponse
                        .builder()
                        .tags(Translator.translateTagsToSdk(TAGS))
                        .build();

        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .packageArn(PKG_ARN)
                .description(PKG_DESC)
                .tags(TAGS)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .clientRequestToken("TOKEN")
                .build();

        when(iotClient.createPackage(any(CreatePackageRequest.class))).thenReturn(createPackageResponse);
        when(iotClient.getPackage(any(GetPackageRequest.class))).thenReturn(getPackageResponse);
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(listTagsForResourceResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }


    @Test
    public void handleRequest_Success_No_Name() {
        final CreatePackageResponse createPackageResponse =
                CreatePackageResponse
                        .builder()
                        .packageName("Generated Name")
                        .packageArn(PKG_ARN)
                        .description(PKG_DESC)
                        .build();

        final GetPackageResponse getPackageResponse =
                GetPackageResponse
                        .builder()
                        .packageName("Generated Name")
                        .packageArn(PKG_ARN)
                        .description(PKG_DESC)
                        .defaultVersionName(DEFAULT_VER_NAME)
                        .build();

        final ListTagsForResourceResponse listTagsForResourceResponse = ListTagsForResourceResponse.builder().tags(Collections.emptyList()).build();

        final ResourceModel model = ResourceModel.builder()
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("TOKEN")
                .build();

        when(iotClient.createPackage(any(CreatePackageRequest.class))).thenReturn(createPackageResponse);
        when(iotClient.getPackage(any(GetPackageRequest.class))).thenReturn(getPackageResponse);
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(listTagsForResourceResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getPackageName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Create_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.createPackage(any(CreatePackageRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).createPackage(any(CreatePackageRequest.class));
    }

    @Test
    public void handleRequest_Create_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.createPackage(any(CreatePackageRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).createPackage(any(CreatePackageRequest.class));
    }

    @Test
    public void handleRequest_Create_ResourceAlreadyExistsException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.createPackage(any(CreatePackageRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().build());

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).createPackage(any(CreatePackageRequest.class));
    }

    @Test
    public void handleRequest_Create_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.createPackage(any(CreatePackageRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).createPackage(any(CreatePackageRequest.class));
    }

    @Test
    public void handleRequest_Create_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.createPackage(any(CreatePackageRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).createPackage(any(CreatePackageRequest.class));
    }

    @Test
    public void handleRequest_Create_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.createPackage(any(CreatePackageRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).createPackage(any(CreatePackageRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackage(any(GetPackageRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackage(any(GetPackageRequest.class));
    }

    @Test
    public void handleRequest_Describe_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackage(any(GetPackageRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .packageName(PKG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.getPackage(any(GetPackageRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).getPackage(any(GetPackageRequest.class));
    }
}
