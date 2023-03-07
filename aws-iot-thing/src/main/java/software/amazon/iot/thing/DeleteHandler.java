package software.amazon.iot.thing;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeleteThingRequest;
import software.amazon.awssdk.services.iot.model.DeleteThingResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
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
 * The handler deletes the THING resource (if it exists)
 * API Calls for DeleteHandler:
 * DescribeThing: To check whether the resource exists; throw "NotFound" status code otherwise
 * DeleteThing: To delete a Thing
 */
public class DeleteHandler extends BaseHandlerStd {

    private static final String OPERATION = "DeleteThing";
    private static final String CALL_GRAPH = "AWS-IoT-Thing::Delete";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        if (StringUtils.isEmpty(resourceModel.getThingName())) {
            throw new CfnNotFoundException(InvalidRequestException.builder()
                    .message("Parameter 'ThingName' must be provided.")
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
     * @param deleteThingRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeleteThingResponse deleteResource(
            DeleteThingRequest deleteThingRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            checkForThing(deleteThingRequest.thingName(), proxyClient);
            DeleteThingResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    deleteThingRequest, proxyClient.client()::deleteThing);
            logger.log(String.format("%s [%s] successfully deleted.",
                    ResourceModel.TYPE_NAME, deleteThingRequest.thingName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deleteThingRequest.thingName(), OPERATION, e);
        }
    }

    private Boolean stabilizedOnDelete(
            DeleteThingRequest deleteThingRequest,
            DeleteThingResponse deleteThingResponse,
            ProxyClient<IotClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        try {
            checkForThing(deleteThingRequest.thingName(), proxyClient);
            return false;
        } catch (CfnNotFoundException e) {
            return true;
        }
    }

    private void checkForThing(String thingName, ProxyClient<IotClient> proxyClient) {
        try {
            final DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                    .thingName(thingName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(describeThingRequest, proxyClient.client()::describeThing);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(thingName, OPERATION, e);
            }
        }
    }
}
