package software.amazon.iot.thinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateDynamicThingGroupResponse;
import software.amazon.awssdk.services.iot.model.CreateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateThingGroupResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidQueryException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThingGroupMetadata;
import software.amazon.awssdk.services.iot.model.ThingGroupProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {
    CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateThingGroupResponse createThingGroupResponse =
                CreateThingGroupResponse
                        .builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build();
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class))).thenReturn(createThingGroupResponse);
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build());

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
        final CreateThingGroupResponse createThingGroupResponse =
                CreateThingGroupResponse
                        .builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName("Generated Name")
                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("client request token")
                .build();

        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class))).thenReturn(createThingGroupResponse);
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName("Generated Name")
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getThingGroupName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AllProperties() {
        final CreateThingGroupResponse createThingGroupResponse =
                CreateThingGroupResponse
                        .builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .build();

        Set<software.amazon.iot.thinggroup.Tag> tags = new HashSet<>();
        tags.add(software.amazon.iot.thinggroup.Tag.builder().key("key1").value("val1").build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder().key("key2").value("val2").build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder().key("key3").value("val3").build());
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder().key("key1").value("val1").build());
        apiResponseTags.add(Tag.builder().key("key2").value("val2").build());
        apiResponseTags.add(Tag.builder().key("key3").value("val3").build());
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .parentGroupName("ParentGroup")
                .tags(tags)
                .build();

        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class))).thenReturn(createThingGroupResponse);
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .thingGroupMetadata(ThingGroupMetadata.builder().parentGroupName("ParentGroup").build())
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription(TG_DESCRIPTION)
                                .build())
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

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
    public void handleRequest_ResourceConflictFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().resourceId(TG_ID).build());

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
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().thingGroupName(TG_NAME).build());

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
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
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
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
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
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_AccessDeniedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void handleRequest_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_SimpleSuccess_DynamicThingGroup() {
        final CreateDynamicThingGroupResponse createDynamicThingGroupResponse =
                CreateDynamicThingGroupResponse
                        .builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .queryString(DG_QUERYSTRING)
                        .build();
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenReturn(createDynamicThingGroupResponse);
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .queryString(DG_QUERYSTRING)
                        .build());

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
    public void handleRequest_Success_No_Name_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .queryString(DG_QUERYSTRING)
                .build();
        final CreateDynamicThingGroupResponse createDynamicThingGroupResponse =
                CreateDynamicThingGroupResponse
                        .builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName("Generated Name")
                        .queryString(DG_QUERYSTRING)
                        .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("client request token")
                .build();

        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenReturn(createDynamicThingGroupResponse);
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName("Generated Name")
                        .queryString(DG_QUERYSTRING)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getThingGroupName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AllProperties_DynamicThingGroup() {
        final CreateDynamicThingGroupResponse createDynamicThingGroupResponse =
                CreateDynamicThingGroupResponse
                        .builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName("Generated Name")
                        .queryString(DG_QUERYSTRING)
                        .build();

        Set<software.amazon.iot.thinggroup.Tag> tags = new HashSet<>();
        tags.add(software.amazon.iot.thinggroup.Tag.builder().key("key1").value("val1").build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder().key("key2").value("val2").build());
        tags.add(software.amazon.iot.thinggroup.Tag.builder().key("key3").value("val3").build());
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder().key("key1").value("val1").build());
        apiResponseTags.add(Tag.builder().key("key2").value("val2").build());
        apiResponseTags.add(Tag.builder().key("key3").value("val3").build());
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .thingGroupProperties(software.amazon.iot.thinggroup.ThingGroupProperties.builder()
                        .thingGroupDescription(TG_DESCRIPTION)
                        .build())
                .parentGroupName("ParentGroup")
                .queryString(DG_QUERYSTRING)
                .tags(tags)
                .build();

        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenReturn(createDynamicThingGroupResponse);
        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribeThingGroupResponse.builder()
                        .thingGroupArn(TG_ARN)
                        .thingGroupId(TG_ID)
                        .thingGroupName(TG_NAME)
                        .thingGroupMetadata(ThingGroupMetadata.builder().parentGroupName("ParentGroup").build())
                        .thingGroupProperties(ThingGroupProperties.builder()
                                .thingGroupDescription(TG_DESCRIPTION)
                                .build())
                        .queryString(DG_QUERYSTRING)
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

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
    public void handleRequest_ResourceConflictFails_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().resourceId(TG_ID).build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_ResourceAlreadyExistsWithSamePropertyFails_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().thingGroupName(TG_NAME).build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }

    @Test
    public void handleRequest_InvalidRequestFails_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_InternalExceptionFails_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_ThrottlingFails_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_AccessDeniedException_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void handleRequest_ServiceUnavailableException_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_ServiceInvalidQueryException_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(InvalidQueryException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_ServiceLimitExceededException_DynamicThingGroup() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(LimitExceededException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceLimitExceeded);
    }
}
