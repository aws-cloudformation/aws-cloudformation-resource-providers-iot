package software.amazon.iot.billinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudformation.model.OperationStatus;
import software.amazon.awssdk.services.iot.model.GroupNameAndArn;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListBillingGroupsRequest;
import software.amazon.awssdk.services.iot.model.ListBillingGroupsResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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

        when(iotClient.listBillingGroups(any(ListBillingGroupsRequest.class)))
                .thenReturn(ListBillingGroupsResponse.builder()
                        .billingGroups(GroupNameAndArn.builder().groupArn(BG_ARN).groupName(BG_Name).build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus().toString()).isEqualTo(OperationStatus.SUCCESS.toString());
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_List_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listBillingGroups(any(ListBillingGroupsRequest.class)))
                .thenThrow(InternalFailureException.class);

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listBillingGroups(any(ListBillingGroupsRequest.class));
    }

    @Test
    public void handleRequest_List_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listBillingGroups(any(ListBillingGroupsRequest.class)))
                .thenThrow(InvalidRequestException.class);

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listBillingGroups(any(ListBillingGroupsRequest.class));
    }

    @Test
    public void handleRequest_List_ResourceNotFoundException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listBillingGroups(any(ListBillingGroupsRequest.class)))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listBillingGroups(any(ListBillingGroupsRequest.class));
    }

    @Test
    public void handleRequest_List_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.listBillingGroups(any(ListBillingGroupsRequest.class)))
                .thenThrow(ThrottlingException.class);

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).listBillingGroups(any(ListBillingGroupsRequest.class));
    }
}
