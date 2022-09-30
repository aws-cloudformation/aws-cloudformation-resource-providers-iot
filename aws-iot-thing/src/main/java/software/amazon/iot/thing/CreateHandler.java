package software.amazon.iot.thing;

import com.amazonaws.util.StringUtils;
import org.apache.commons.collections.MapUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingRequest;
import software.amazon.awssdk.services.iot.model.CreateThingResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
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

    private static final String OPERATION = "CreateThing";
    private static final String CALL_GRAPH = "AWS-IoT-Thing::Create";
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
        if (StringUtils.isNullOrEmpty(resourceModel.getThingName())) {
            resourceModel.setThingName(generateName(request));
        }
        resourceModel.setId(resourceModel.getThingName());

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createResource)
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(resourceModel));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createThingRequest     the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateThingResponse createResource(
            final CreateThingRequest createThingRequest,
            final ProxyClient<IotClient> proxyClient) {

        try {
            checkForThing(createThingRequest.thingName(), proxyClient);
            final CreateThingResponse createThingResponse = proxyClient.injectCredentialsAndInvokeV2(
                    createThingRequest, proxyClient.client()::createThing);
            logger.log(String.format("%s [%s] successfully created.",
                    ResourceModel.TYPE_NAME, createThingRequest.thingName()));
            return createThingResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createThingRequest.thingName(), OPERATION, e);
        }
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

    private void checkForThing(final String thingName, final ProxyClient<IotClient> proxyClient) {
        try {
            final DescribeThingRequest describeThingRequest = DescribeThingRequest.builder()
                    .thingName(thingName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(describeThingRequest, proxyClient.client()::describeThing);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, thingName);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN &&
                    !(e instanceof UnauthorizedException || e instanceof ResourceNotFoundException)) {
                throw Translator.translateIotExceptionToHandlerException(thingName, OPERATION, e);
            }
        }
    }
}
