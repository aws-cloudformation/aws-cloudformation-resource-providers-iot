package software.amazon.iot.thingtype;

import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
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
 * The handler creates the THING resource - if the name is not provided, it is auto-generated
 * The thing type resource is UN-DEPRECATED by default - unless specified in the template
 *
 * API Calls for CreateHandler:
 * DescribeThing: To check whether the resource does not exist; throw "AlreadyExists" status code otherwise
 * CreateThingType: To create a new ThingType
 * DeprecateThingType: To deprecate a ThingType (if "DeprecateThingType" property is true)
 */
public class CreateHandler extends BaseHandlerStd {

    private static final int MAX_THING_TYPE_NAME_LENGTH = 128;
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

        // create a thing type name if not provided by user
        if (resourceModel.getThingTypeName() == null) {
            resourceModel.setThingTypeName(generateName(request));
        }

        final DescribeThingTypeRequest describeThingTypeRequest = Translator.translateToReadRequest(resourceModel);
        final CreateThingTypeRequest createThingTypeRequest = Translator.translateToCreateRequest(resourceModel, tags);
        final DeprecateThingTypeRequest deprecateThingTypeRequest = Translator.translateToDeprecateRequest(resourceModel, false);

        try {
            try {
                // check whether the resource exists - ResourceNotFound is thrown otherwise.
                proxyClient.injectCredentialsAndInvokeV2(
                        describeThingTypeRequest,
                        proxyClient.client()::describeThingType
                );
                logger.log(String.format("%s %s already exists. Failing create handler.",
                        ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName()));
                throw new ResourceAlreadyExistsException(new Exception(
                        String.format("%s %s already exists. Failing create handler.",
                                ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName())));
            } catch (ResourceNotFoundException resourceNotFoundException) {
                logger.log(String.format("%s %s does not exist. Continuing to create resource.",
                        ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName()));
            }

            // create the resource
            proxyClient.injectCredentialsAndInvokeV2(
                    createThingTypeRequest,
                    proxyClient.client()::createThingType
            );
            logger.log(String.format("%s %s successfully created.",
                    ResourceModel.TYPE_NAME, createThingTypeRequest.thingTypeName()));

            // deprecate the thing type if specified in the stack template
            proxyClient.injectCredentialsAndInvokeV2(
                    deprecateThingTypeRequest,
                    proxyClient.client()::deprecateThingType
            );
            logger.log(String.format("%s %s has successfully been updated with specified deprecation status.",
                    ResourceModel.TYPE_NAME, deprecateThingTypeRequest.thingTypeName()));
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
                "THING_TYPE" :
                request.getLogicalResourceIdentifier());
        return IdentifierUtils.generateResourceIdentifier(
                identifierPrefix.toString(),
                request.getClientRequestToken(),
                MAX_THING_TYPE_NAME_LENGTH);
    }
}
