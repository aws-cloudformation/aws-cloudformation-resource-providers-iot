package software.amazon.iot.billinggroup;

import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

/**
 * API Calls for CreateHandler:
 * CreateBillingGroup: To create a new BillingGroup
 */
public class CreateHandler extends BaseHandlerStd {

    private static final int MAX_BILLING_GROUP_NAME_LENGTH = 128;
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel resourceModel = request.getDesiredResourceState();
        final Map<String,String> tags = request.getDesiredResourceTags();

        // create a billing group name if not provided by user
        if (resourceModel.getBillingGroupName() == null) {
            resourceModel.setBillingGroupName(generateName(request));
        }

        final DescribeBillingGroupRequest describeBillingGroupRequest = Translator.translateToReadRequest(resourceModel);
        final CreateBillingGroupRequest createBillingGroupRequest = Translator.translateToCreateRequest(resourceModel, tags);

        try {
            try {
                // check whether the resource exists - ResourceNotFound is thrown otherwise.
                proxyClient.injectCredentialsAndInvokeV2(
                        describeBillingGroupRequest,
                        proxyClient.client()::describeBillingGroup
                );
                logger.log(String.format("%s %s already exists. Failing create handler.",
                        ResourceModel.TYPE_NAME,describeBillingGroupRequest.billingGroupName()));
                throw new ResourceAlreadyExistsException(new Exception(
                        String.format("%s %s already exists. Failing create handler.",
                        ResourceModel.TYPE_NAME,describeBillingGroupRequest.billingGroupName())));
            } catch (ResourceNotFoundException resourceNotFoundException) {
                logger.log(String.format("%s %s does not exist. Continuing to create resource.",
                        ResourceModel.TYPE_NAME,describeBillingGroupRequest.billingGroupName()));
            }

            proxyClient.injectCredentialsAndInvokeV2(
                    createBillingGroupRequest, proxyClient.client()::createBillingGroup);
            logger.log(String.format("%s %s successfully created.",
                    ResourceModel.TYPE_NAME, createBillingGroupRequest.billingGroupName()));
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        final StringBuilder identifierPrefix = new StringBuilder();
        identifierPrefix.append((request.getSystemTags() != null &&
                MapUtils.isNotEmpty(request.getSystemTags())) ?
                request.getSystemTags().get("aws:cloudformation:stack-name") + "-" : "");
        identifierPrefix.append(request.getLogicalResourceIdentifier() == null ?
                "BILLING_GROUP" :
                request.getLogicalResourceIdentifier());
        return IdentifierUtils.generateResourceIdentifier(
                identifierPrefix.toString(),
                request.getClientRequestToken(),
                MAX_BILLING_GROUP_NAME_LENGTH);
    }
}
