package software.amazon.iot.thinggroup;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteDynamicThingGroupResponse;
import software.amazon.awssdk.services.iot.model.DeleteThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
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
 * DeleteThingGroup: To delete a ThingGroup
 * DeleteDynamicThingGroup: To delete a Dynamic ThingGroup
 * DescribeThingGroup: used for the following purpose -
 *  - To verify whether the thing group exists
 *  - To check whether the ThingGroup to be deleted has a queryString; Dynamic Thing group would have the param set
 */
public class DeleteHandler extends BaseHandlerStd {

    private static final String OPERATION = "DeleteThingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-ThingGroup::Delete";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        if (StringUtils.isEmpty(resourceModel.getThingGroupName())) {
            throw new CfnNotFoundException(InvalidRequestException.builder()
                    .message("Parameter 'ThingGroupName' must be provided.")
                    .build());
        }

        if (isDynamicThingGroup(checkForThingGroup(resourceModel.getThingGroupName(), proxyClient, OPERATION))){
            return ProgressEvent.progress(resourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                    .translateToServiceRequest(Translator::translateToDeleteDynamicThingGroupRequest)
                                    .makeServiceCall(this::deleteDynamicThingGroupResource)
                                    .stabilize(this::stabilizedDynamicThingGroupOnDelete)
                                    .done(response -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                            .status(OperationStatus.SUCCESS)
                                            .build()));
        } else {
            return ProgressEvent.progress(resourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                    .translateToServiceRequest(Translator::translateToDeleteThingGroupRequest)
                                    .makeServiceCall(this::deleteThingGroupResource)
                                    .stabilize(this::stabilizedThingGroupOnDelete)
                                    .done(response -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                            .status(OperationStatus.SUCCESS)
                                            .build()));
        }
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deleteThingGroupRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteThingGroupResponse deleteThingGroupResource(
            DeleteThingGroupRequest deleteThingGroupRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DeleteThingGroupResponse deleteThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    deleteThingGroupRequest, proxyClient.client()::deleteThingGroup);
            logger.log(String.format("%s [%s] successfully deleted.",
                    ResourceModel.TYPE_NAME, deleteThingGroupRequest.thingGroupName()));
            return deleteThingGroupResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deleteThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deleteDynamicThingGroupRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteDynamicThingGroupResponse deleteDynamicThingGroupResource(
            DeleteDynamicThingGroupRequest deleteDynamicThingGroupRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DeleteDynamicThingGroupResponse deleteDynamicThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    deleteDynamicThingGroupRequest, proxyClient.client()::deleteDynamicThingGroup);
            logger.log(String.format("%s [%s] successfully deleted.",
                    ResourceModel.TYPE_NAME, deleteDynamicThingGroupRequest.thingGroupName()));
            return deleteDynamicThingGroupResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deleteDynamicThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    private Boolean stabilizedThingGroupOnDelete(
            DeleteThingGroupRequest deleteThingGroupRequest,
            DeleteThingGroupResponse deleteThingGroupResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        try {
            checkForThingGroup(deleteThingGroupRequest.thingGroupName(), proxyClient, OPERATION);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }

    private Boolean stabilizedDynamicThingGroupOnDelete(
            DeleteDynamicThingGroupRequest deleteDynamicThingGroupRequest,
            DeleteDynamicThingGroupResponse deleteDynamicThingGroupResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        try {
            checkForThingGroup(deleteDynamicThingGroupRequest.thingGroupName(), proxyClient, OPERATION);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }
}
