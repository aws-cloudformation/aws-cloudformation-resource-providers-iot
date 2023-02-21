package software.amazon.iot.thingtype;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListThingTypesRequest;
import software.amazon.awssdk.services.iot.model.ListThingTypesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ListHandler:
 * ListThingTypes: To retrieve a list of all ThingTypes in the account
 */
public class ListHandler extends BaseHandlerStd {

    private static final String OPERATION = "ListThingTypes";

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        try {
            final ListThingTypesRequest listThingTypesRequest = Translator.translateToListRequest(request.getNextToken());
            ListThingTypesResponse listThingTypesResponse = proxy.injectCredentialsAndInvokeV2(
                    listThingTypesRequest,
                    proxyClient.client()::listThingTypes
            );
            String nextToken = listThingTypesResponse.nextToken();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(listThingTypesResponse))
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(null, OPERATION, e);
        }
    }
}
