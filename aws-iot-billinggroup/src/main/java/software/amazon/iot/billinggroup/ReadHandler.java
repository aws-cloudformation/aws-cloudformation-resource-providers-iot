package software.amazon.iot.billinggroup;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
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
 * DescribeBillingGroup: To retrieve all properties of a new/updated BillingGroup
 * ListTagsForResource: To retrieve all tags associated with BillingGroup
 */
public class ReadHandler extends BaseHandlerStd {

    private static final String OPERATION = "DescribeBillingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-BillingGroup::Read";
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
                .done((describeBillingGroupRequest, describeBillingGroupResponse, sdkProxyClient, model, context) ->
                        constructResourceModelFromResponse(sdkProxyClient, describeBillingGroupResponse));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param describeBillingGroupRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private DescribeBillingGroupResponse readResource(
            DescribeBillingGroupRequest describeBillingGroupRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DescribeBillingGroupResponse describeBillingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeBillingGroupRequest, proxyClient.client()::describeBillingGroup);
            logger.log(String.format("%s [%s] has successfully been read.",
                    ResourceModel.TYPE_NAME, describeBillingGroupRequest.billingGroupName()));
            return describeBillingGroupResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(describeBillingGroupRequest.billingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param describeBillingGroupResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            ProxyClient<IotClient> proxyClient,
            DescribeBillingGroupResponse describeBillingGroupResponse) {
        final ResourceModel resourceModel = Translator.translateFromReadResponse(describeBillingGroupResponse);

        try {
            List<Tag> tags = listTags(proxyClient, describeBillingGroupResponse.billingGroupArn());
            resourceModel.setTags(Translator.translateTagsFromSdk(tags));
        } catch (final IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(
                        describeBillingGroupResponse.billingGroupName(), OPERATION, e);
            }
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
