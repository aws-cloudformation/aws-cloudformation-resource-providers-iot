package software.amazon.iot.softwarepackageversion;

import com.amazonaws.iot.cfn.common.handler.Tagging;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageResponse;
import software.amazon.awssdk.services.iot.model.GetPackageVersionRequest;
import software.amazon.awssdk.services.iot.model.GetPackageVersionResponse;
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
 * GetPackageVersions: To retrieve all properties of a new/updated PackageVersion
 * ListTagsForResource: To retrieve all tags associated with a PackageVersion
 */
public class ReadHandler extends BaseHandlerStd {

    private static final String OPERATION = "GetSoftwarePackageVersion";
    private static final String CALL_GRAPH = "AWS-IoT-SoftwarePackageVersion::Read";
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
                .done((getPackageVersionRequest, getPackageVersionResponse, sdkProxyClient, model, context) ->
                        constructSuccessfulProgressEventFromResponse(sdkProxyClient, getPackageVersionResponse));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param getPackageVersionRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private GetPackageVersionResponse readResource(
            GetPackageVersionRequest getPackageVersionRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            GetPackageVersionResponse getPackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(
                    getPackageVersionRequest, proxyClient.client()::getPackageVersion);
            logger.log(String.format("%s [%s, %s] has successfully been read.",
                    ResourceModel.TYPE_NAME, getPackageVersionRequest.packageName(), getPackageVersionRequest.versionName()));
            return getPackageVersionResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(getPackageVersionRequest.packageName() + ":" + getPackageVersionRequest.versionName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param getPackageVersionResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructSuccessfulProgressEventFromResponse(
            ProxyClient<IotClient> proxyClient,
            GetPackageVersionResponse getPackageVersionResponse) {
        final ResourceModel resourceModel = Translator.translateFromReadResponse(getPackageVersionResponse);

        try {
            List<Tag> tags = listTags(proxyClient, getPackageVersionResponse.packageVersionArn());
            resourceModel.setTags(Tagging.translateSdkTagsToMap(tags));
        } catch (final IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(
                        getPackageVersionResponse.packageName() + ":" + getPackageVersionResponse.versionName(), OPERATION, e);
            }
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
