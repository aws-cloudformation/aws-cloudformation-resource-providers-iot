package software.amazon.iot.thinggroup;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListThingGroupsRequest;
import software.amazon.awssdk.services.iot.model.ListThingGroupsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ListHandler:
 * ListThingGroups: To retrieve a list of all ThingGroups in the account
 */
public class ListHandler extends BaseHandlerStd {

    private static final String OPERATION = "ListThingGroups";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();

        try {
            final ListThingGroupsRequest listThingGroupsRequest = Translator.translateToListRequest(request.getNextToken());
            ListThingGroupsResponse listThingGroupsResponse =
                    proxyClient.injectCredentialsAndInvokeV2(
                            listThingGroupsRequest,
                            proxyClient.client()::listThingGroups
                    );
            String nextToken = listThingGroupsResponse.nextToken();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(listThingGroupsResponse))
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(null, OPERATION, e);
        }
    }
}
