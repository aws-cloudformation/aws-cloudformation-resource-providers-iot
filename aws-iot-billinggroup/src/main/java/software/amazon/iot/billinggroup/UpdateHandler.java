package software.amazon.iot.billinggroup;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeBillingGroupResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.UpdateBillingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateBillingGroupResponse;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * API Calls for UpdateHandler:
 * UpdateBillingGroup: To update the properties of BillingGroup
 * DescribeBillingGroup: To retrieve ARN of the BillingGroup to make Tag and UnTag API calls
 * ListTagsForResource: To retrieve old tags associated with BillingGroup
 * UntagResource: To remove old tags
 * TagResource: To add new tags
 */
public class UpdateHandler extends BaseHandlerStd {

    private static final String OPERATION = "UpdateBillingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-BillingGroup::Update";
    private static final String CALL_GRAPH_TAG = "AWS-IoT-BillingGroup::Tagging";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel prevModel = request.getPreviousResourceState() == null ?
                request.getDesiredResourceState() : request.getPreviousResourceState();
        ResourceModel newModel = request.getDesiredResourceState();

        validatePropertiesAreUpdatable(newModel, prevModel);

        return ProgressEvent.progress(newModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newModel, callbackContext)
                                .translateToServiceRequest(Translator::translateToUpdateRequest)
                                .makeServiceCall(this::updateResource)
                                .progress())
                .then(progress -> updateResourceTags(proxy, proxyClient, progress, request))
                .then(progress -> ProgressEvent.defaultSuccessHandler(newModel));
    }

    private void validatePropertiesAreUpdatable(ResourceModel newModel, ResourceModel prevModel) {
        if (!StringUtils.equals(newModel.getBillingGroupName(), prevModel.getBillingGroupName())) {
            throwCfnNotUpdatableException("BillingGroupName");
        } else if (StringUtils.isNotEmpty(newModel.getArn()) && !StringUtils.equals(newModel.getArn(), prevModel.getArn())) {
            throwCfnNotUpdatableException("Arn");
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidRequestException.builder()
                .message(String.format("Parameter '%s' is not updatable.", propertyName))
                .build());
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param updateBillingGroupRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdateBillingGroupResponse updateResource(
            UpdateBillingGroupRequest updateBillingGroupRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            final UpdateBillingGroupResponse updateBillingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updateBillingGroupRequest, proxyClient.client()::updateBillingGroup);
            logger.log(String.format("%s [%s] has been successfully updated.",
                    ResourceModel.TYPE_NAME, updateBillingGroupRequest.billingGroupName()));
            return updateBillingGroupResponse;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updateBillingGroupRequest.billingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation to update resource tags through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param proxy
     * @param proxyClient
     * @param progress
     * @param request
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateResourceTags(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<IotClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceHandlerRequest<ResourceModel> request) {
        return proxy.initiate(CALL_GRAPH_TAG, proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getRequest, proxyInvocation) -> {
                    try {
                        DescribeBillingGroupResponse describeBillingGroupResponse = proxyInvocation.injectCredentialsAndInvokeV2(getRequest,
                                proxyInvocation.client()::describeBillingGroup);

                        final String resourceArn = describeBillingGroupResponse.billingGroupArn();

                        // Desired tags (including user-defined and stack tags)
                        final Map<String, String> desiredTags = new HashMap<>();
                        Optional.ofNullable(request.getDesiredResourceTags())
                                .ifPresent(desiredTags::putAll);
                        Optional.ofNullable(request.getDesiredResourceState())
                                .map(ResourceModel::getTags)
                                .map(Translator::translateTagstoMap)
                                .ifPresent(desiredTags::putAll);
                        final Set<Tag> desiredTagSet = Translator.translateTagsToSdk(desiredTags);

                        // Existing resource State tags (including user-defined and stack tags)
                        final Map<String, String> existingTags = new HashMap<>();
                        Optional.ofNullable(request.getPreviousResourceState())
                                .map(ResourceModel::getTags)
                                .map(Translator::translateTagstoMap)
                                .ifPresent(existingTags::putAll);
                        Optional.ofNullable(request.getPreviousResourceTags())
                                .ifPresent(existingTags::putAll);
                        final Set<Tag> existingTagSet = Translator.translateTagsToSdk(existingTags);

                        final Set<Tag> tagsToRemove = Sets.difference(existingTagSet, desiredTagSet);
                        final Set<Tag> tagsToAdd = Sets.difference(desiredTagSet, existingTagSet);

                        if (!tagsToRemove.isEmpty()) {
                            proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.untagResourceRequest(resourceArn, tagsToRemove),
                                    proxyClient.client()::untagResource
                            );
                            logger.log(String.format("%s [%s] untagResourceRequest successfully completed.",
                                    ResourceModel.TYPE_NAME, resourceArn));
                        }
                        if (!tagsToAdd.isEmpty()) {
                            proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.tagResourceRequest(resourceArn, tagsToAdd),
                                    proxyClient.client()::tagResource
                            );
                            logger.log(String.format("%s [%s] tagResourceRequest successfully completed.",
                                    ResourceModel.TYPE_NAME, resourceArn));
                        }
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    } catch (IotException e) {
                        throw Translator.translateIotExceptionToHandlerException(getRequest.billingGroupName(), OPERATION, e);
                    }
                })
                .progress();
    }
}
