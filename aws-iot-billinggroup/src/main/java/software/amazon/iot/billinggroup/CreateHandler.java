package software.amazon.iot.billinggroup;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.CreateBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupRequest;
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

import java.util.Map;

/**
 * The handler creates the BILLING-GROUP resource - if the name is not provided, it is auto-generated
 * API Calls for CreateHandler:
 * DescribeBillingGroup: To check whether the resource does not exist; throw "AlreadyExists" status code otherwise
 * CreateBillingGroup: To create a new Billing group
 */
public class CreateHandler extends BaseHandlerStd {

    private static final String OPERATION = "CreateBillingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-BillingGroup::Create";
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
        if (StringUtils.isNullOrEmpty(resourceModel.getBillingGroupName())) {
            resourceModel.setBillingGroupName(generateName(request));
        }
        resourceModel.setId(resourceModel.getBillingGroupName());

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, resourceModel, callbackContext)
                                .translateToServiceRequest(model-> Translator.translateToCreateRequest(resourceModel, tags))
                                .makeServiceCall(this::createResource)
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(resourceModel));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createBillingGroupRequest the aws service request to create a resource
     * @param proxyClient the aws service client to make the call
     * @return awsResponse create resource response
     */
    private CreateBillingGroupResponse createResource(
            final CreateBillingGroupRequest createBillingGroupRequest,
            final ProxyClient<IotClient> proxyClient) {

        try {
            checkForBillingGroup(createBillingGroupRequest.billingGroupName(), proxyClient);
            final CreateBillingGroupResponse createBillingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    createBillingGroupRequest, proxyClient.client()::createBillingGroup);
            logger.log(String.format("%s [%s] successfully created.",
                    ResourceModel.TYPE_NAME, createBillingGroupRequest.billingGroupName()));
            return createBillingGroupResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(createBillingGroupRequest.billingGroupName(), OPERATION, e);
        }
    }

    private void checkForBillingGroup(final String billingGroupName, final ProxyClient<IotClient> proxyClient) {
        try {
            final DescribeBillingGroupRequest describeBillingGroupRequest = DescribeBillingGroupRequest.builder()
                    .billingGroupName(billingGroupName)
                    .build();
            proxyClient.injectCredentialsAndInvokeV2(describeBillingGroupRequest, proxyClient.client()::describeBillingGroup);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, billingGroupName);
        } catch (IotException e) {
            if (e.statusCode() != HttpStatusCode.FORBIDDEN &&
                    !(e instanceof UnauthorizedException || e instanceof ResourceNotFoundException)) {
                throw Translator.translateIotExceptionToHandlerException(billingGroupName, OPERATION, e);
            }
        }
    }

    private String generateName(final ResourceHandlerRequest<ResourceModel> request) {
        return IdentifierUtils.generateResourceIdentifier(
                StringUtils.isNullOrEmpty(request.getLogicalResourceIdentifier()) ?
                        "ThingGroup" : request.getLogicalResourceIdentifier(),
                request.getClientRequestToken(),
                MAX_BILLING_GROUP_NAME_LENGTH).replace("-", "_");
    }
}
