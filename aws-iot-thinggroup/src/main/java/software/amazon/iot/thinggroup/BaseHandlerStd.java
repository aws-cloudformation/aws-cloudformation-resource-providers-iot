package software.amazon.iot.thinggroup;

import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger);

    protected boolean isDynamicThingGroup(DescribeThingGroupResponse response) {
        return response.queryString() != null;
    }

    protected boolean isDynamicThingGroup(ResourceModel resourceModel) {
        return resourceModel.getQueryString() != null;
    }

    protected List<Tag> listTags(final ProxyClient<IotClient> proxyClient, final String arn) {
        String nextToken = null;
        List<Tag> listOfTags = new ArrayList<>();
        do {
            final ListTagsForResourceResponse response = proxyClient.injectCredentialsAndInvokeV2(
                    Translator.listResourceTagsRequest(arn, nextToken),
                    proxyClient.client()::listTagsForResource);
            listOfTags.addAll(response.tags());
            nextToken = response.nextToken();
        } while (nextToken != null);
        return listOfTags;
    }

    protected DescribeThingGroupResponse checkForThingGroup(
            final String thingGroupName,
            final ProxyClient<IotClient> proxyClient,
            final String operation) {
        try {
            final DescribeThingGroupRequest describeThingGroupRequest = DescribeThingGroupRequest.builder()
                    .thingGroupName(thingGroupName)
                    .build();
            return proxyClient.injectCredentialsAndInvokeV2(
                    describeThingGroupRequest, proxyClient.client()::describeThingGroup);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(thingGroupName, operation, e);
            }
            return null;
        }
    }
}
