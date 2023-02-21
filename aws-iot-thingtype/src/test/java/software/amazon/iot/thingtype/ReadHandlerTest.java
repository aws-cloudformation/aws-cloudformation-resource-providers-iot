package software.amazon.iot.thingtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThingTypeMetadata;
import software.amazon.awssdk.services.iot.model.ThingTypeProperties;
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
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    ReadHandler handler= new ReadHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .thingTypeProperties(software.amazon.iot.thingtype.ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        ResourceModel expectedModel = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .id(TT_ID)
                .arn(TT_ARN)
                .deprecateThingType(false)
                .thingTypeProperties(software.amazon.iot.thingtype.ThingTypeProperties.builder()
                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                        .searchableAttributes(Collections.emptyList())
                        .build())
                .tags(new HashSet<>())
                .build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder()
                                .deprecated(false)
                                .build())
                        .thingTypeProperties(ThingTypeProperties.builder()
                                .thingTypeDescription(THING_TYPE_DESCRIPTION)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder()
                        .build());

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
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
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
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
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
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
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
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_Describe_UnauthorizedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenThrow(UnauthorizedException.class);

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient, never()).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder()
                                .deprecated(false)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder()
                                .deprecated(false)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder()
                                .deprecated(false)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }

    @Test
    public void handleRequest_ListTags_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .thingTypeName(TT_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeThingType(any(DescribeThingTypeRequest.class)))
                .thenReturn(DescribeThingTypeResponse.builder()
                        .thingTypeArn(TT_ARN)
                        .thingTypeId(TT_ID)
                        .thingTypeName(TT_Name)
                        .thingTypeMetadata(ThingTypeMetadata.builder()
                                .deprecated(false)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeThingType(any(DescribeThingTypeRequest.class));
        verify(iotClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    }
}
