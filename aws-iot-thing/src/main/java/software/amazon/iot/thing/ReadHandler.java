package software.amazon.iot.thing;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ReadHandler:
 * DescribeThing: To retrieve all properties of a new/updated Thing
 */
public class ReadHandler extends BaseHandlerStd {

    private static final String OPERATION = "DescribeThing";
    private static final String CALL_GRAPH = "AWS-IoT-Thing::Read";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();

        return proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readResource)
                .done((describeThingRequest, describeThingResponse, sdkProxyClient, model, context) ->
                        constructResourceModelFromResponse(describeThingResponse));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param describeThingRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private DescribeThingResponse readResource(
            DescribeThingRequest describeThingRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DescribeThingResponse describeThingResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingRequest, proxyClient.client()::describeThing);
            logger.log(String.format("%s [%s] has successfully been read.",
                    ResourceModel.TYPE_NAME, describeThingRequest.thingName()));
            return describeThingResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(describeThingRequest.thingName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param describeThingResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            DescribeThingResponse describeThingResponse) {
        final ResourceModel resourceModel = Translator.translateFromReadResponse(describeThingResponse);
        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
