package software.amazon.iot.softwarepackage;

import com.amazonaws.util.StringUtils;
import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreatePackageRequest;
import software.amazon.awssdk.services.iot.model.CreatePackageResponse;
import software.amazon.awssdk.services.iot.model.CreatePackageVersionRequest;
import software.amazon.awssdk.services.iot.model.CreatePackageVersionResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.UpdatePackageRequest;
import software.amazon.awssdk.services.iot.model.UpdatePackageResponse;
import software.amazon.awssdk.services.iot.model.UpdatePackageVersionRequest;
import software.amazon.awssdk.services.iot.model.UpdatePackageVersionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.HashMap;
import java.util.Map;

public class CreateHandler extends BaseHandlerStd {
    private final static String OPERATION = "CreateSoftwarePackage";
    private static final String CALL_GRAPH = "AWS-IoT-SoftwarePackage::Create";
    private static final int MAX_PACKAGE_NAME_LENGTH = 128;
    private Logger logger;
    private String clientToken;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        this.clientToken = request.getClientRequestToken();

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final Map<String, String> stackTags = request.getDesiredResourceTags();
        // TODO: aws: System tags not supported by our tagging operation
        // final Map<String, String> systemTags = request.getSystemTags();
        final String awsAccountId = request.getAwsAccountId();

        logger.log(String.format("%s for accountId: %s",
                OPERATION, awsAccountId));

        Map<String, String> combinedTags = new HashMap<>();
        Map<String, String> modelTags = Translator.translateTagsToSdk(resourceModel.getTags());
        if (stackTags != null) {
            combinedTags.putAll(stackTags);
        }
        if (modelTags != null) {
            combinedTags.putAll(modelTags);
        }

        // create a package name if not provided by user
        if (StringUtils.isNullOrEmpty(resourceModel.getPackageName())) {
            resourceModel.setPackageName(generateName(request));
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(model -> Translator.translateToCreateRequest(resourceModel, combinedTags))
                                .makeServiceCall(this::createResource)
                                .progress())
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createPackageRequest     the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreatePackageResponse createResource(
            final CreatePackageRequest createPackageRequest,
            final ProxyClient<IotClient> proxyClient) {
        // TODO: add in client token once idempotency is implemented for the API
        CreatePackageRequest requestWithClientToken = CreatePackageRequest.builder()
                .packageName(createPackageRequest.packageName())
                .description(createPackageRequest.description())
                .tags(createPackageRequest.tags())
                //.clientToken(this.clientToken)
                .build();
        try {
            final CreatePackageResponse createPackageResponse = proxyClient.injectCredentialsAndInvokeV2(
                    requestWithClientToken, proxyClient.client()::createPackage);
            logger.log(String.format("%s [%s] successfully created.",
                    ResourceModel.TYPE_NAME, createPackageRequest.packageName()));
            return createPackageResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createPackageRequest.packageName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createPackageVersionRequest     the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreatePackageVersionResponse createResourceForPackageVersion(
            final CreatePackageVersionRequest createPackageVersionRequest,
            final ProxyClient<IotClient> proxyClient) {
        try {
            final CreatePackageVersionResponse createPackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(
                    createPackageVersionRequest, proxyClient.client()::createPackageVersion);
            logger.log(String.format("%s [%s, %s] successfully created.",
                    ResourceModel.TYPE_NAME, createPackageVersionRequest.packageName(), createPackageVersionRequest.versionName()));
            return createPackageVersionResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createPackageVersionRequest.packageName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param updatePackageVersionRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse update resource response
     */
    private UpdatePackageVersionResponse updateResourceForPackageVersion(
            final UpdatePackageVersionRequest updatePackageVersionRequest,
            final ProxyClient<IotClient> proxyClient) {
        try {
            final UpdatePackageVersionResponse updatePackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updatePackageVersionRequest, proxyClient.client()::updatePackageVersion);
            logger.log(String.format("%s [%s, %s] successfully updated.",
                    ResourceModel.TYPE_NAME, updatePackageVersionRequest.packageName(), updatePackageVersionRequest.versionName()));
            return updatePackageVersionResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updatePackageVersionRequest.packageName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param updatePackageRequest     the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse update resource response
     */
    private UpdatePackageResponse updateResourceForDefaultVersionName(
            UpdatePackageRequest updatePackageRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            final UpdatePackageResponse updatePackageResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updatePackageRequest, proxyClient.client()::updatePackage);
            logger.log(String.format("%s [%s] has been successfully updated.",
                    ResourceModel.TYPE_NAME, updatePackageRequest.packageName()));
            return updatePackageResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updatePackageRequest.packageName(), OPERATION, e);
        }
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        final StringBuilder identifierPrefix = new StringBuilder();
        identifierPrefix.append((request.getSystemTags() != null &&
                MapUtils.isNotEmpty(request.getSystemTags())) ?
                request.getSystemTags().get("aws:cloudformation:stack-name") + "-" : "");
        identifierPrefix.append(request.getLogicalResourceIdentifier() == null ?
                "SOFTWARE-PACKAGE" :
                request.getLogicalResourceIdentifier());

        return IdentifierUtils.generateResourceIdentifier(
                identifierPrefix.toString(),
                request.getClientRequestToken(),
                MAX_PACKAGE_NAME_LENGTH);
    }

}
