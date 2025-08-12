package software.amazon.iot.billinggroup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iot.model.CreateBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
public class CreateHandlerTest extends AbstractTestBase {
    final CreateHandler handler = new CreateHandler();

    @Test
    public void handleRequest_Success() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .id(BG_ID)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenReturn(CreateBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
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
        final CreateBillingGroupResponse createBillingGroupResponse =
                CreateBillingGroupResponse
                        .builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName("Generated Name")
                        .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("client request token")
                .build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenReturn(createBillingGroupResponse);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getBillingGroupName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleCreate_withResourceAndStackTags() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .id(BG_ID)
                .billingGroupProperties(software.amazon.iot.billinggroup.BillingGroupProperties.builder()
                        .billingGroupDescription(BILLING_GROUP_DESCRIPTION)
                        .build())
                .tags(new HashSet<>(Collections.singletonList(
                        Tag.builder().key("resourceKey1").value("resourceValue1").build()
                )))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("LRI")
                .clientRequestToken("client request token")
                .desiredResourceTags(Collections.singletonMap("stackKey1", "stackValue1"))
                .build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.class);
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenReturn(CreateBillingGroupResponse.builder()
                        .billingGroupArn(BG_ARN)
                        .billingGroupId(BG_ID)
                        .billingGroupName(BG_Name)
                        .build());

        // Capture the CreateBillingGroup request to verify parameters
        ArgumentCaptor<CreateBillingGroupRequest> createRequestCaptor =
                ArgumentCaptor.forClass(CreateBillingGroupRequest.class);

        final ProgressEvent<ResourceModel, CallbackContext> response =
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, LOGGER);

        verify(iotClient).createBillingGroup(createRequestCaptor.capture());
        CreateBillingGroupRequest createRequest = createRequestCaptor.getValue();
        assertThat(createRequest.billingGroupName()).isEqualTo(BG_Name);
        assertThat(createRequest.tags()).hasSize(2);
        assertThat(createRequest.tags()).contains(
                software.amazon.awssdk.services.iot.model.Tag.builder().key("stackKey1").value("stackValue1").build(),
                software.amazon.awssdk.services.iot.model.Tag.builder().key("resourceKey1").value("resourceValue1").build()
        );
        assertThat(createRequest.billingGroupProperties().billingGroupDescription()).isEqualTo(BILLING_GROUP_DESCRIPTION);

        verify(iotClient).createBillingGroup(any(CreateBillingGroupRequest.class));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel().getBillingGroupName()).isNotNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_Create_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_ResourceAlreadyExistsException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().build());

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Create_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());
        when(iotClient.createBillingGroup(any(CreateBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ResourceExists() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenReturn(DescribeBillingGroupResponse.builder().build());

        assertThrows(CfnAlreadyExistsException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InternalFailureException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InternalFailureException.builder().build());

        assertThrows(CfnInternalFailureException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_InvalidRequestException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(InvalidRequestException.builder().build());

        assertThrows(CfnInvalidRequestException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).createBillingGroup(any(CreateBillingGroupRequest.class));
    }

    @Test
    public void handleRequest_Describe_ThrottlingException() {
        final ResourceModel model = ResourceModel.builder()
                .billingGroupName(BG_Name)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = defaultRequestBuilder(model).build();

        when(iotClient.describeBillingGroup(any(DescribeBillingGroupRequest.class)))
                .thenThrow(ThrottlingException.builder().build());

        assertThrows(CfnThrottlingException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(),proxyClient,LOGGER));
        verify(iotClient).describeBillingGroup(any(DescribeBillingGroupRequest.class));
        verify(iotClient, never()).createBillingGroup(any(CreateBillingGroupRequest.class));
    }
}
