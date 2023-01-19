package software.amazon.iot.thinggroup;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
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
 * DescribeThingGroup: To retrieve all properties of a new/updated ThingGroup
 * ListTagsForResource: To retrieve all tags associated with ThingGroup
 */
public class ReadHandler extends BaseHandlerStd {

    private static final String OPERATION = "DescribeThingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-ThingGroup::Read";
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
                        constructResourceModelFromResponse(describeThingResponse, proxyClient));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param describeThingGroupRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private DescribeThingGroupResponse readResource(
            DescribeThingGroupRequest describeThingGroupRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DescribeThingGroupResponse describeThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingGroupRequest, proxyClient.client()::describeThingGroup);
            logger.log(String.format("%s [%s] has successfully been read.",
                    ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName()));
            return describeThingGroupResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(
                    describeThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the list tags request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings and construct the response
     * @param describeThingGroupResponse the aws service describe resource response
     * @param proxyInvocation the aws service client to make the call
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final DescribeThingGroupResponse describeThingGroupResponse,
            final ProxyClient<IotClient> proxyInvocation) {
        final ResourceModel resourceModel = Translator.translateFromReadResponse(describeThingGroupResponse);
        try {
            List<Tag> tags = listTags(proxyInvocation, describeThingGroupResponse.thingGroupArn());
            resourceModel.setTags(Translator.translateTagsFromSdk(tags));
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(
                        describeThingGroupResponse.thingGroupName(), OPERATION, e);
            }
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }

}
