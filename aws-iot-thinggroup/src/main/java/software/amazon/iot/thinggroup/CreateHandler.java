package software.amazon.iot.thinggroup;

import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
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
 * CreateThingGroup: To create a new ThingGroup
 * CreateDynamicThingGroup: To create a new Dynamic Thing Group (if queryString is provided in the resource template)
 * DescribeThingGroup: To verify whether the resource already exists
 */
public class CreateHandler extends BaseHandlerStd {

    private static final int MAX_THING_GROUP_NAME_LENGTH = 128;
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

        // create a thing group name if not provided by user
        if (resourceModel.getThingGroupName() == null) {
            resourceModel.setThingGroupName(generateName(request));
        }

        final DescribeThingGroupRequest describeThingGroupRequest = Translator.translateToReadRequest(resourceModel);

        try {
            try {
                // check whether the resource exists - ResourceNotFound is thrown otherwise.
                proxyClient.injectCredentialsAndInvokeV2(
                        describeThingGroupRequest,
                        proxyClient.client()::describeThingGroup
                );
                logger.log(String.format("%s %s already exists. Failing create handler.",
                        ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName()));
                throw new ResourceAlreadyExistsException(new Exception(
                        String.format("%s %s already exists. Failing create handler.",
                                ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName())));
            } catch (ResourceNotFoundException resourceNotFoundException) {
                logger.log(String.format("%s %s does not exist. Continuing to create resource.",
                        ResourceModel.TYPE_NAME, describeThingGroupRequest.thingGroupName()));
            }

            // route the create request based on whether dynamic group related attributes are provided
            if (isDynamicThingGroup(resourceModel)) {
                final CreateDynamicThingGroupRequest createDynamicThingGroupRequest =
                        Translator.translateToCreateDynamicThingGroupRequest(resourceModel, tags);

                proxyClient.injectCredentialsAndInvokeV2(
                        createDynamicThingGroupRequest, proxyClient.client()::createDynamicThingGroup);
                logger.log(String.format("%s %s (DynamicThingGroup) successfully created.",
                        ResourceModel.TYPE_NAME, createDynamicThingGroupRequest.thingGroupName()));
            } else {
                final CreateThingGroupRequest createThingGroupRequest =
                        Translator.translateToCreateThingGroupRequest(resourceModel, tags);

                proxyClient.injectCredentialsAndInvokeV2(
                        createThingGroupRequest, proxyClient.client()::createThingGroup);
                logger.log(String.format("%s %s successfully created.",
                        ResourceModel.TYPE_NAME, createThingGroupRequest.thingGroupName()));
            }
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
        String resourceType = (isDynamicThingGroup(request.getDesiredResourceState())) ? "DYNAMIC_THING_GROUP" : "THING_GROUP";
        identifierPrefix.append(request.getLogicalResourceIdentifier() == null ?
                resourceType :
                request.getLogicalResourceIdentifier());
        return IdentifierUtils.generateResourceIdentifier(
                identifierPrefix.toString(),
                request.getClientRequestToken(),
                MAX_THING_GROUP_NAME_LENGTH);
    }
}
