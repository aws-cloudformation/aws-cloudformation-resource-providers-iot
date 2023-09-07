package software.amazon.iot.softwarepackage;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.UpdateIndexingConfigurationRequest;
import software.amazon.awssdk.services.iot.model.UpdateIndexingConfigurationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {
    public static final String DEFAULT_PACKAGE_VERSION_NAME = "cloudformation-default-package-version";

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

    protected List<Tag> listTags(final ProxyClient<IotClient> proxyClient, final String arn) {
        String nextToken = null;
        List<Tag> listOfTags = new ArrayList<>();
        do {
            final ListTagsForResourceResponse response = proxyClient.injectCredentialsAndInvokeV2(Translator.listResourceTagsRequest(arn, nextToken),
                    proxyClient.client()::listTagsForResource);
            listOfTags.addAll(response.tags());
            nextToken = response.nextToken();
        } while (nextToken != null);
        return listOfTags;
    }

    protected UpdateIndexingConfigurationResponse updateIndexingConfiguration(
            final UpdateIndexingConfigurationRequest updateIndexingConfigurationRequest,
            final ProxyClient<IotClient> proxyClient) {
        try {
            final UpdateIndexingConfigurationResponse updateIndexingConfigurationResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updateIndexingConfigurationRequest, proxyClient.client()::updateIndexingConfiguration);
            return updateIndexingConfigurationResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updateIndexingConfigurationRequest.thingIndexingConfiguration().thingIndexingModeAsString(), "UpdateIndexingConfiguration", e);
        }
    }
}
