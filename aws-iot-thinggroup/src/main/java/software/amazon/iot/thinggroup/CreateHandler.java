package software.amazon.iot.thinggroup;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateDynamicThingGroupResponse;
import software.amazon.awssdk.services.iot.model.CreateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateThingGroupResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupRequest;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

/**
 * The handler creates the THING-GROUP resource - if the name is not provided, it is auto-generated
 * API Calls for CreateHandler:
 * CreateThingGroup: To create a new ThingGroup
 * CreateDynamicThingGroup: To create a new Dynamic Thing Group (if queryString is provided in the resource template)
 * DescribeThingGroup: To verify whether the resource already exists
 */
public class CreateHandler extends BaseHandlerStd {

    private static final String OPERATION = "CreateThingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-ThingGroup::Create";
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

        validateProperties(resourceModel);

        // create a thing group name if not provided by user
        if (StringUtils.isNullOrEmpty(resourceModel.getThingGroupName())) {
            resourceModel.setThingGroupName(generateName(request));
        }

        if (isDynamicThingGroup(resourceModel)) {
            return ProgressEvent.progress(resourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                    .translateToServiceRequest(model -> Translator.translateToCreateDynamicThingGroupRequest(resourceModel, tags))
                                    .makeServiceCall(this::createDynamicThingGroupResource)
                                    .progress())
                    .then(progress -> ProgressEvent.defaultSuccessHandler(resourceModel));
        } else {
            return ProgressEvent.progress(resourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                    .translateToServiceRequest(model -> Translator.translateToCreateThingGroupRequest(resourceModel, tags))
                                    .makeServiceCall(this::createThingGroupResource)
                                    .progress())
                    .then(progress -> ProgressEvent.defaultSuccessHandler(resourceModel));
        }
    }

    /**
     * Perform checks to confirm that all properties defined in the template are correct
     * Examples:
     * 1. ThingGroup resource cannot have "queryString" and "parentThingGroup" defined - as dynamic thing group
     * do not support hierarchy
     * @param resourceModel
     */
    private void validateProperties(ResourceModel resourceModel) {
        if (!StringUtils.isNullOrEmpty(resourceModel.getQueryString())
                && !StringUtils.isNullOrEmpty(resourceModel.getParentGroupName())) {
            throw new CfnInvalidRequestException(InvalidRequestException.builder()
                    .message("Thing group cannot have a QueryString and a ParentGroup")
                    .build());
        }
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createThingGroupRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateThingGroupResponse createThingGroupResource(
            final CreateThingGroupRequest createThingGroupRequest,
            final ProxyClient<IotClient> proxyClient) {

        try {
            checkForThingGroup(createThingGroupRequest.thingGroupName(), proxyClient);
            final CreateThingGroupResponse createThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    createThingGroupRequest, proxyClient.client()::createThingGroup);
            logger.log(String.format("%s [%s] successfully created.",
                    ResourceModel.TYPE_NAME, createThingGroupRequest.thingGroupName()));
            return createThingGroupResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createDynamicThingGroupRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateDynamicThingGroupResponse createDynamicThingGroupResource(
            final CreateDynamicThingGroupRequest createDynamicThingGroupRequest,
            final ProxyClient<IotClient> proxyClient) {

        try {
            checkForThingGroup(createDynamicThingGroupRequest.thingGroupName(), proxyClient);
            final CreateDynamicThingGroupResponse createDynamicThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    createDynamicThingGroupRequest, proxyClient.client()::createDynamicThingGroup);
            logger.log(String.format("%s [%s] successfully created.",
                    ResourceModel.TYPE_NAME, createDynamicThingGroupRequest.thingGroupName()));
            return createDynamicThingGroupResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createDynamicThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    private void checkForThingGroup(final String thingGroupName, final ProxyClient<IotClient> proxyClient) {
        try {
            final DescribeThingGroupRequest describeThingGroupRequest = DescribeThingGroupRequest.builder()
                    .thingGroupName(thingGroupName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(describeThingGroupRequest, proxyClient.client()::describeThingGroup);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, thingGroupName);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN &&
                    !(e instanceof UnauthorizedException || e instanceof ResourceNotFoundException)) {
                throw Translator.translateIotExceptionToHandlerException(thingGroupName, OPERATION, e);
            }
        }
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                StringUtils.isNullOrEmpty(request.getLogicalResourceIdentifier()) ?
                        "ThingGroup" : request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_THING_GROUP_NAME_LENGTH).replace("-", "_");
    }
}
