package software.amazon.iot.billinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.BillingGroupProperties;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    ReadHandler handler= new ReadHandler();

    @Test
    public void handleRequest_SimpleSuccess() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();
        ResourceModel expectedModel = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .id(BG_ID)
                .arn(BG_ARN)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .tags(new HashSet<>())
                .build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .billingGroupProperties(BillingGroupProperties.builder()
                                .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
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
        org.assertj.core.api.Assertions.assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Describe_InternalFailure() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_ListTags_InternalFailure() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .billingGroupProperties(BillingGroupProperties.builder()
                                .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                                .build())
                        .build());
        when(iotClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void handleRequest_InvalidRequest() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void handleRequest_ResourceNotFound() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void handleRequest_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }
}
