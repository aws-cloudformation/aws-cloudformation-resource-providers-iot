package software.amazon.iot.thingtype;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

/**
 * API Calls for ReadHandler:
 * DescribeThingType: To retrieve all properties of a new/updated ThingType
 * ListTagsForResource: To retrieve all tags associated with ThingType
 */
public class ReadHandler extends BaseHandlerStd {

    private static final String OPERATION = "DescribeThingType";
    private static final String CALL_GRAPH = "AWS-IoT-ThingType::Read";
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
                    .done((describeThingTypeRequest, describeThingTypeResponse, sdkProxyClient, model, context) ->
                            constructResourceModelFromResponse(describeThingTypeResponse, proxyClient));

    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param describeThingTypeRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private DescribeThingTypeResponse readResource(
            DescribeThingTypeRequest describeThingTypeRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DescribeThingTypeResponse describeThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingTypeRequest, proxyClient.client()::describeThingType);

            logger.log(String.format("%s [%s] has successfully been read.",
                    ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName()));
            return describeThingTypeResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    describeThingTypeRequest.thingTypeName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the list tags request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings and construct the response
     * @param describeThingTypeResponse the aws service describe resource response
     * @param proxyClient the aws service client to make the call
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            DescribeThingTypeResponse describeThingTypeResponse,
            ProxyClient<IotClient> proxyClient) {
        final ResourceModel resourceModel = Translator.translateFromReadResponse(describeThingTypeResponse);


        try {
            List<Tag> tags = listTags(proxyClient, describeThingTypeResponse.thingTypeArn());

            resourceModel.setTags(Translator.translateTagsFromSdk(tags));
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(
                        describeThingTypeResponse.thingTypeName(), OPERATION, e);
            }
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
