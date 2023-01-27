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
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidQueryException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThingGroupMetadata;
import software.amazon.awssdk.services.iot.model.ThingGroupProperties;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
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
        assertThat(response.getResourceModel().getTags().size()).isEqualTo(apiResponseTags.size());
    }

    @Test
    public void handleRequest_ResourceAlreadyExistsWithSamePropertyFails() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenReturn(DescribeThingGroupResponse.builder().thingGroupName(TG_NAME).build());

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createThingGroup(any(CreateThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createThingGroup(any(CreateThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_ResourceAlreadyExistsException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.class);

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createThingGroup(any(CreateThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createThingGroup(any(CreateThingGroupRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createThingGroup(any(CreateThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
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
    public void handleRequest_Create_DynamicThingGroup_FailsWithParentGroupPresent() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .parentGroupName(TG_PARENT_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient, never()).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
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
        assertThat(response.getResourceModel().getTags().size()).isEqualTo(apiResponseTags.size());
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_InvalidQueryException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(InvalidQueryException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_LimitExceededException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(LimitExceededException.class);

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_ResourceAlreadyExistsException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.class);

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }

    @Test
    public void handleRequest_DynamicThingGroupCreate_FleetIndexingNotEnabled() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .queryString(DG_QUERYSTRING)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ResourceNotFoundException resourceNotFoundException = ResourceNotFoundException.builder()
                .message("AWS IoT Fleet Indexing is not enabled. Please enable index by calling UpdateIndexingConfiguration.")
                .build();

        when(iotClient.describeThingGroup(any(DescribeThingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class)))
                .thenThrow(resourceNotFoundException);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingGroup(any(DescribeThingGroupRequest.class));
        verify(iotClient).createDynamicThingGroup(any(CreateDynamicThingGroupRequest.class));
    }
}
