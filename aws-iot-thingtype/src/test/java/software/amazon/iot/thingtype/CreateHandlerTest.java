package software.amazon.iot.thingtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.CreateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    public void handleRequest_AllProperties() {
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
                        .build())
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingType(any(CreateThingTypeRequest.class))).thenReturn(createThingTypeResponse);

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
                        .thingTypeName("Generated Name")
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
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().resourceId(TT_ID).build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_ResourceAlreadyExistsWithSamePropertyFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().thingTypeName(TT_Name).build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(InternalException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingType(any(CreateThingTypeRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }
}
