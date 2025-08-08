package software.amazon.iot.thingtype;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.CreateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The handler creates the THING-TYPE resource - if the name is not provided, it is auto-generated
 * The thing type resource is UN-DEPRECATED by default - unless specified in the template
 *
 * API Calls for CreateHandler:
 * DescribeThingType: To check whether the resource does not exist; throw "AlreadyExists" status code otherwise
 * CreateThingType: To create a new ThingType
 * DeprecateThingType: To deprecate a ThingType (if "DeprecateThingType" property is true)
 */
public class CreateHandler extends BaseHandlerStd {

    private static final String OPERATION = "CreateThingType";
    private static final String CALL_GRAPH = "AWS-IoT-ThingType::Create";
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
        // consolidate all tags
        final Map<String, String> tags = new HashMap<>();
        // add user-defined tags in model
        Optional.ofNullable(resourceModel.getTags())
                .ifPresent(modelTags -> tags.putAll(Translator.translateTagstoMap(modelTags)));
        // add stack-level tags
        Optional.ofNullable(request.getDesiredResourceTags()).ifPresent(tags::putAll);

        // create a thing type name if not provided by user
        if (StringUtils.isNullOrEmpty(resourceModel.getThingTypeName())) {
            resourceModel.setThingTypeName(generateName(request));
        }

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(model -> Translator.translateToCreateRequest(resourceModel, tags))
                                .makeServiceCall(this::createThingTypeResource)
                                .done((response) -> {
                                    resourceModel.setId(response.thingTypeId());
                                    resourceModel.setArn(response.thingTypeArn());
                                    return progress;
                                })
                )
                .then(progress -> checkForDeprecate(proxy, proxyClient, progress, request, resourceModel))
                .then(progress -> ProgressEvent.defaultSuccessHandler(resourceModel));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createThingTypeRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateThingTypeResponse createThingTypeResource(
            CreateThingTypeRequest createThingTypeRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            checkForThingType(createThingTypeRequest.thingTypeName(), proxyClient);
            final CreateThingTypeResponse createThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                    createThingTypeRequest, proxyClient.client()::createThingType);
            logger.log(String.format("%s [%s] successfully created.",
                    ResourceModel.TYPE_NAME, createThingTypeRequest.thingTypeName()));
            return createThingTypeResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createThingTypeRequest.thingTypeName(), OPERATION, e);
        }
    }

    private void checkForThingType(String thingTypeName, ProxyClient<IotClient> proxyClient) {
        try {
            final DescribeThingTypeRequest describeThingTypeRequest = DescribeThingTypeRequest.builder()
                    .thingTypeName(thingTypeName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(describeThingTypeRequest, proxyClient.client()::describeThingType);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, thingTypeName);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN &&
                    !(e instanceof UnauthorizedException || e instanceof ResourceNotFoundException)) {
                throw Translator.translateIotExceptionToHandlerException(thingTypeName, OPERATION, e);
            }
        }
    }

    /**
     * Implement client invocation to deprecate thing type (if specified) through the proxyClient, which is already
     * initialised with caller credentials, correct region and retry settings
     */
    private ProgressEvent<ResourceModel, CallbackContext> checkForDeprecate(
            AmazonWebServicesClientProxy proxy,
            ProxyClient<IotClient> proxyClient,
            ProgressEvent<ResourceModel, CallbackContext> progress,
            ResourceHandlerRequest<ResourceModel> request,
            ResourceModel resourceModel) {
        return proxy.initiate(CALL_GRAPH, proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getRequest, proxyInvocation) -> {
                    try {
                        if (resourceModel.getDeprecateThingType() != null && resourceModel.getDeprecateThingType()) {
                            // deprecate the thing type if specified in the stack template
                            proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.translateToDeprecateRequest(resourceModel, false),
                                    proxyClient.client()::deprecateThingType
                            );
                        }
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    } catch (IotException e) {
                        throw Translator.translateIotExceptionToHandlerException(getRequest.thingTypeName(), OPERATION, e);
                    }
                })
                .progress();
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                StringUtils.isNullOrEmpty(request.getLogicalResourceIdentifier()) ?
                        "ThingType" : request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_THING_TYPE_NAME_LENGTH).replace("-", "_");
    }
}
