package software.amazon.iot.thinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.OperationStatus;
import software.amazon.awssdk.services.iot.model.GroupNameAndArn;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListThingGroupsRequest;
import software.amazon.awssdk.services.iot.model.ListThingGroupsResponse;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends AbstractTestBase{

    ListHandler handler = new ListHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listThingGroups(any(ListThingGroupsRequest.class)))
                .thenReturn(ListThingGroupsResponse.builder()
                        .thingGroups(GroupNameAndArn.builder().groupName(TG_NAME).groupArn(TG_ARN).build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        org.assertj.core.api.Assertions.assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_InternalFailure() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingGroups(any(ListThingGroupsRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingGroups(any(ListThingGroupsRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(null).build();

        when(iotClient.listThingGroups(any(ListThingGroupsRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void handleRequest_AccessDeniedException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listThingGroups(any(ListThingGroupsRequest.class)))
                .thenThrow(UnauthorizedException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void handleRequest_ServiceUnavailableException() {
        final ResourceModel model = ResourceModel.builder()
                .thingGroupName(TG_NAME)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listThingGroups(any(ListThingGroupsRequest.class)))
                .thenThrow(ServiceUnavailableException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(software.amazon.cloudformation.proxy.OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }
}