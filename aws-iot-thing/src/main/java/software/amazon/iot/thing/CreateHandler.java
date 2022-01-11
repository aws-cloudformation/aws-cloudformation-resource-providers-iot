package software.amazon.iot.thing;

import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

/**
 * The handler creates the THING resource - if the name is not provided, it is auto-generated
 * API Calls for CreateHandler:
 * DescribeThing: To check whether the resource does not exist; throw "AlreadyExists" status code otherwise
 * CreateThing: To create a new Thing
 */
public class CreateHandler extends BaseHandlerStd {

    private static final int MAX_THING_NAME_LENGTH = 128;
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();

        // create a thing name if not provided by user
        if (resourceModel.getThingName() == null) {
            resourceModel.setThingName(generateName(request));
        }

        final DescribeThingRequest describeThingRequest = Translator.translateToReadRequest(resourceModel);
        final CreateThingRequest createThingRequest = Translator.translateToCreateRequest(resourceModel);

        try {
            try {
                // check whether the resource exists - ResourceNotFound is thrown otherwise.
                proxyClient.injectCredentialsAndInvokeV2(
                        describeThingRequest,
                        proxyClient.client()::describeThing
                );
                logger.log(String.format("%s %s already exists. Failing create handler.",
                        ResourceModel.TYPE_NAME, describeThingRequest.thingName()));
                throw new ResourceAlreadyExistsException(new Exception(
                        String.format("%s %s already exists. Failing create handler.",
                                ResourceModel.TYPE_NAME,  describeThingRequest.thingName())));
            } catch (ResourceNotFoundException resourceNotFoundException) {
                logger.log(String.format("%s %s does not exist. Continuing to create resource.",
                        ResourceModel.TYPE_NAME,  describeThingRequest.thingName()));
            }

            proxyClient.injectCredentialsAndInvokeV2(
                    createThingRequest,
                    proxyClient.client()::createThing);
            logger.log(String.format("%s %s successfully created.",
                    ResourceModel.TYPE_NAME, createThingRequest.thingName()));
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
                "THING" :
                request.getLogicalResourceIdentifier());

        return IdentifierUtils.generateResourceIdentifier(
                identifierPrefix.toString(),
                request.getClientRequestToken(),
                MAX_THING_NAME_LENGTH);
    }
}
