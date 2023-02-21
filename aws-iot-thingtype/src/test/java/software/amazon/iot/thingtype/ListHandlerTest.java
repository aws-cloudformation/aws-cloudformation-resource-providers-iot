package software.amazon.iot.thingtype;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListThingTypesRequest;
import software.amazon.awssdk.services.iot.model.ListThingTypesResponse;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThingTypeDefinition;
import software.amazon.awssdk.services.iot.model.ThingTypeMetadata;
import software.amazon.awssdk.services.iot.model.ThingTypeProperties;
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
public class ListHandlerTest extends AbstractTestBase{

    ListHandler handler = new ListHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listThingTypes(any(ListThingTypesRequest.class)))
                .thenReturn(ListThingTypesResponse.builder()
                        .thingTypes(ThingTypeDefinition.builder()
                                .thingTypeArn(TT_ARN)
                                .thingTypeName(TT_Name)
                                .thingTypeMetadata(ThingTypeMetadata.builder()
                                        .deprecated(false)
                                        .build())
                                .thingTypeProperties(ThingTypeProperties.builder()
                                        .thingTypeDescription(THING_TYPE_DESCRIPTION)
                                        .build())
                                .build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalFailureException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingTypes(any(ListThingTypesRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listThingTypes(any(ListThingTypesRequest.class));
    }

    @Test
    public void handleRequest_InvalidRequestException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingTypes(any(ListThingTypesRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listThingTypes(any(ListThingTypesRequest.class));
    }

    @Test
    public void handleRequest_ServiceUnavailableException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingTypes(any(ListThingTypesRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listThingTypes(any(ListThingTypesRequest.class));
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingTypes(any(ListThingTypesRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listThingTypes(any(ListThingTypesRequest.class));
    }

    @Test
    public void handleRequest_UnauthorizedException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingTypes(any(ListThingTypesRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        assertThrows(CfnAccessDeniedException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listThingTypes(any(ListThingTypesRequest.class));
    }
}
