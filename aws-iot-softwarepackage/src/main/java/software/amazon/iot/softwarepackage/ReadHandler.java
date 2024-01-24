package software.amazon.iot.softwarepackage;

import com.amazonaws.iot.cfn.common.handler.Tagging;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageResponse;
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
 * GetPackage: To retrieve all properties of a new/updated Package
 * ListTagsForResource: To retrieve all tags associated with a Package
 */
public class ReadHandler extends BaseHandlerStd {

    private static final String OPERATION = "GetSoftwarePackage";
    private static final String CALL_GRAPH = "AWS-IoT-SoftwarePackage::Read";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final String awsAccountId = request.getAwsAccountId();

        logger.log(String.format("%s for accountId: %s",
                OPERATION, awsAccountId));

        return proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall(this::readResource)
                .done((getPackageRequest, getPackageResponse, sdkProxyClient, model, context) ->
                        constructSuccessfulProgressEventFromResponse(sdkProxyClient, getPackageResponse));
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param getPackageRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private GetPackageResponse readResource(
            GetPackageRequest getPackageRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            GetPackageResponse getPackageResponse = proxyClient.injectCredentialsAndInvokeV2(
                    getPackageRequest, proxyClient.client()::getPackage);
            logger.log(String.format("%s [%s] has successfully been read.",
                    ResourceModel.TYPE_NAME, getPackageRequest.packageName()));
            return getPackageResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(getPackageRequest.packageName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param getPackageResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructSuccessfulProgressEventFromResponse(
            ProxyClient<IotClient> proxyClient,
            GetPackageResponse getPackageResponse) {
        final ResourceModel resourceModel = Translator.translateFromReadResponse(getPackageResponse);

        try {
            List<Tag> tags = listTags(proxyClient, getPackageResponse.packageArn());
            resourceModel.setTags(Translator.translateTagsToCfn(tags));
        } catch (final IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN) {
                throw Translator.translateIotExceptionToHandlerException(
                        getPackageResponse.packageName(), OPERATION, e);
            }
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
