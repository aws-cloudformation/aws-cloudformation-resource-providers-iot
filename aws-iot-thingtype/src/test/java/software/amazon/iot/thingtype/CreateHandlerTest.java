package software.amazon.iot.thingtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.CreateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateThingTypeResponse createThingTypeResponse =
                CreateThingTypeResponse
                        .builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .build();
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.createThingType(any(CreateThingTypeRequest.class))).thenReturn(createThingTypeResponse);
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

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
    public void handleRequest_SuccessWithAllProperties() {
        final CreateThingTypeResponse createThingTypeResponse =
                CreateThingTypeResponse
                        .builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .build();
        List<String> searchableAttributes = Arrays.asList("A1", "A2", "A3");
        Set<software.amazon.iot.thingtype.Tag> tags = new HashSet<>();
        tags.add(software.amazon.iot.thingtype.Tag.builder().key("key1").value("val1").build());
        tags.add(software.amazon.iot.thingtype.Tag.builder().key("key2").value("val2").build());
        tags.add(software.amazon.iot.thingtype.Tag.builder().key("key3").value("val3").build());
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .searchableAttributes(searchableAttributes)
                        .mqtt5Configuration(Mqtt5Configuration.builder()
                                .propagatingAttributes(Collections.singletonList(PropagatingAttribute.builder()
                                        .userPropertyKey("testPropagatingAttribute")
                                        .connectionAttribute("iot:Thing.ThingName")
                                        .build()))
                                .build())
                        .build())
                .tags(tags)
                .deprecateThingType(false)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingType(any(CreateThingTypeRequest.class))).thenReturn(createThingTypeResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_CreateDeprecate() {
        final CreateThingTypeResponse createThingTypeResponse =
                CreateThingTypeResponse
                        .builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .build();
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.createThingType(any(CreateThingTypeRequest.class))).thenReturn(createThingTypeResponse);
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

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
        final ResourceModel model = ResourceModel.builder()
                .build();
        final CreateThingTypeResponse createThingTypeResponse =
                CreateThingTypeResponse
                        .builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName("GeneratedName")
                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("client request token")
                .build();

        when(iotClient.createThingType(any(CreateThingTypeRequest.class))).thenReturn(createThingTypeResponse);
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getThingTypeName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Create_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Create_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Create_ResourceAlreadyExistsException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.class);

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Create_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Create_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Create_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenReturn(CreateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenReturn(CreateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenReturn(CreateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenReturn(CreateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenReturn(CreateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenReturn(CreateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).createThingType(any(CreateThingTypeRequest.class));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }
}
