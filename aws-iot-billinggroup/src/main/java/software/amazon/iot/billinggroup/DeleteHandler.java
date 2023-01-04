package software.amazon.iot.billinggroup;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.cloudwatch.model.InvalidParameterValueException;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for DeleteHandler:
 * DeleteBillingGroup: To delete a BillingGroup
 * DescribeBillingGroup: To check whether the resource exists; throw "NotFound" status code otherwise
 */
public class DeleteHandler extends BaseHandlerStd {

    private static final String OPERATION = "DeleteBillingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-BillingGroup::Delete";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        if (StringUtils.isEmpty(resourceModel.getBillingGroupName())) {
            throw new CfnNotFoundException(InvalidParameterValueException.builder()
                    .message("Parameter 'BillingGroupName' must be provided.")
                    .build());
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.SUCCESS)
                                        .build()));
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deleteBillingGroupRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteBillingGroupResponse deleteResource(
            DeleteBillingGroupRequest deleteBillingGroupRequest,
            ProxyClient<IotClient> proxyClient
    ) {
        try {
            checkForBillingGroup(deleteBillingGroupRequest.billingGroupName(), proxyClient);
            DeleteBillingGroupResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    deleteBillingGroupRequest, proxyClient.client()::deleteBillingGroup);
            logger.log(String.format("%s [%s] successfully deleted.",
                    ResourceModel.TYPE_NAME, deleteBillingGroupRequest.billingGroupName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deleteBillingGroupRequest.billingGroupName(), OPERATION, e);
        }
    }

    private void checkForBillingGroup(String billingGroupName, ProxyClient<IotClient> proxyClient) {
        try {
            final DescribeBillingGroupRequest describeBillingGroupRequest = DescribeBillingGroupRequest.builder()
                    .billingGroupName(billingGroupName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(describeBillingGroupRequest, proxyClient.client()::describeBillingGroup);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(billingGroupName, OPERATION, e);
            }
        }
    }

    private Boolean stabilizedOnDelete(
            DeleteBillingGroupRequest deleteBillingGroupRequest,
            DeleteBillingGroupResponse deleteBillingGroupResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        try {
            checkForBillingGroup(deleteBillingGroupRequest.billingGroupName(), proxyClient);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }
}
