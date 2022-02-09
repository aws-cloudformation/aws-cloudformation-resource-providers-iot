package software.amazon.iot.thing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.CreateThingResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

    CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateThingResponse createThingResponse =
                CreateThingResponse
                        .builder()
                        .thingArn(T_ARN)
                        .thingId(T_ID)
                        .thingName(T_Name)
                        .build();
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.createThing(any(CreateThingRequest.class))).thenReturn(createThingResponse);
        when(iotClient.describeThing(any(DescribeThingRequest.class)))
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
        final CreateThingResponse createThingResponse =
                CreateThingResponse
                        .builder()
                        .thingArn(T_ARN)
                        .thingId(T_ID)
                        .thingName(T_Name)
                        .build();
        Map<String, String> attributes = new HashMap<>();
        attributes.put("attr1", "val1");
        attributes.put("attr2", "val2");
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .attributePayload(AttributePayload.builder().attributes(attributes).build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.createThing(any(CreateThingRequest.class))).thenReturn(createThingResponse);
        when(iotClient.describeThing(any(DescribeThingRequest.class)))
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
        final CreateThingResponse createThingResponse =
                CreateThingResponse
                        .builder()
                        .thingArn(T_ARN)
                        .thingId(T_ID)
                        .thingName("Generated Name")
                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("client request token")
                .build();

        when(iotClient.createThing(any(CreateThingRequest.class))).thenReturn(createThingResponse);
        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getThingName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThing(any(CreateThingRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().resourceId(T_ID).build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_ResourceAlreadyExistsWithSamePropertyFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenReturn(DescribeThingResponse.builder().thingName(T_Name).build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_InvalidRequestFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThing(any(CreateThingRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_InternalExceptionFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThing(any(CreateThingRequest.class)))
                .thenThrow(InternalException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_ThrottlingFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingName(T_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThing(any(DescribeThingRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThing(any(CreateThingRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }
}
