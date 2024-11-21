package software.amazon.iot.thingtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.TagResourceRequest;
import software.amazon.awssdk.services.iot.model.TagResourceResponse;
import software.amazon.awssdk.services.iot.model.ThingTypeMetadata;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.awssdk.services.iot.model.UntagResourceRequest;
import software.amazon.awssdk.services.iot.model.UntagResourceResponse;
import software.amazon.awssdk.services.iot.model.UpdateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingTypeResponse;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    final UpdateHandler handler = new UpdateHandler();

    @Test
    public void handleRequest_DeprecateThingType() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder()
                        .build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder().deprecated(true).build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getDeprecateThingType()).isEqualTo(newModel.getDeprecateThingType());
    }

    @Test
    public void handleRequest_UnDeprecateThingType() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(false)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenReturn(DeprecateThingTypeResponse.builder()
                        .build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder().deprecated(false).build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getDeprecateThingType()).isEqualTo(newModel.getDeprecateThingType());
    }

    @Test
    public void handleRequest_AddTags() {
        Set<software.amazon.iot.thingtype.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String,String> tagMap = new HashMap<>();
        tagMap.put("k1","v1");
        tagMap.put("k2","v2");
        tagMap.put("k3","v3");
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_RemoveAllTags() {
        Set<software.amazon.iot.thingtype.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_AddDeleteAndModifyTags() {
        Set<software.amazon.iot.thingtype.Tag> tags= new HashSet<>();
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        tags.add(software.amazon.iot.thingtype.Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k2")
                .value("v2")
                .build());
        apiResponseTags.add(Tag.builder()
                .key("k3")
                .value("v3")
                .build());
        Map<String,String> tagMap = new HashMap<>();
        tagMap.put("newKey1","v1");
        tagMap.put("newKey2","v2");
        tagMap.put("newKey3","v3");
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .tags(tags)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .desiredResourceTags(tagMap)
                .build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_UpdateThingTypeArn_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .arn("testArn")
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateThingTypeName_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName("updated" + TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateThingTypeProperties_ShouldFail() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateThingTypeDescription_ShouldFailOnRemove() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .build())
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateThingTypeDescription_ShouldFailOnAddition() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .build())
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateThingTypeDescription_ShouldFailOnUpdate() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .thingTypeDescription("Update " + THING_TYPE_DESCRIPTION)
                        .build())
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateSearchableAttributes_ShouldFailOnAddition() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .searchableAttributes(Arrays.asList("A1", "A2", "A3"))
                        .build())
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateSearchableAttributes_ShouldFailOnRemove() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .searchableAttributes(Arrays.asList("A1", "A2", "A3"))
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .build())
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_UpdateSearchableAttributes_ShouldFailOnUpdate() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .searchableAttributes(Arrays.asList("A1", "A2", "A3"))
                        .build())
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(ThingTypeProperties.builder()
                        .searchableAttributes(Arrays.asList("A1", "A2", "A3", "A4"))
                        .build())
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(newModel)
                .previousResourceState(prevModel)
                .build();

        assertThrows(CfnNotUpdatableException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient, never()).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Update_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Update_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Update_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Update_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Update_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Update_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
    }


    @Test
    public void handleRequest_Deprecate_InternalFailureException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_InvalidRequestException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ResourceNotFoundException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ServiceUnavailableException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_ThrottlingException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Deprecate_UnauthorizedException() {
        final ResourceModel prevModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceModel newModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .deprecateThingType(true)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(prevModel, newModel).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.deprecateThingType(any(DeprecateThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).deprecateThingType(any(DeprecateThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ServiceUnavailableException.class);

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_Describe_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
    }

    @Test
    public void handleRequest_ListTags_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InternalFailureException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_InvalidRequestException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_LimitExceededException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(LimitExceededException.class);

        assertThrows(CfnServiceLimitExceededException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ResourceNotFoundException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_TagResource_ThrottlingException() {
        Map<String, String> tags= new HashMap<>();
        tags.put("k1", "v1");

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .desiredResourceTags(tags)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());
        when(iotClient.tagResource(any(TagResourceRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient).tagResource(any(TagResourceRequest.class));
        verify(iotClient, never()).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_InternalFailureException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_InvalidRequestException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_ResourceNotFoundException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }

    @Test
    public void handleRequest_UntagResource_ThrottlingException() {
        Set<Tag> apiResponseTags = new HashSet<>();
        apiResponseTags.add(Tag.builder()
                .key("k1")
                .value("v1")
                .build());

        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model)
                .build();

        when(iotClient.updateThingType(any(UpdateThingTypeRequest.class)))
                .thenReturn(UpdateThingTypeResponse.builder().build());
        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder().build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .tags(apiResponseTags)
                        .build());
        when(iotClient.untagResource(any(UntagResourceRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).updateThingType(any(UpdateThingTypeRequest.class));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
        verify(iotClient, never()).tagResource(any(TagResourceRequest.class));
        verify(iotClient).untagResource(any(UntagResourceRequest.class));
    }
}
