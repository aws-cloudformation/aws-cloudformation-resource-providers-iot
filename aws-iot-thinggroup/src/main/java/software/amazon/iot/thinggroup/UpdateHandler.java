package software.amazon.iot.thinggroup;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingGroupResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.UpdateDynamicThingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateDynamicThingGroupResponse;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingGroupResponse;
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
 * UpdateThingGroup: To update a ThingGroup
 * UpdateDynamicThingGroup: To update a dynamic ThingGroup
 * DescribeThingGroup: To retrieve ARN of the ThingGroup to make Tag and UnTag API calls
 * ListTagsForResource: To retrieve old tags associated with ThingGroup
 * UntagResource: To remove old tags
 * TagResource: To add new tags
 *
 * (Thing-Group cannot be converted to Dynamic-Thing-Group and vice-versa)
 */
public class UpdateHandler extends BaseHandlerStd {

    private static final String OPERATION = "UpdateThingGroup";
    private static final String CALL_GRAPH = "AWS-IoT-ThingGroup::Update";
    private static final String CALL_GRAPH_TAG = "AWS-IoT-ThingGroup::Tagging";
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        ResourceModel prevResourceModel = request.getPreviousResourceState() == null ?
                request.getDesiredResourceState() : request.getPreviousResourceState();
        final ResourceModel newResourceModel = request.getDesiredResourceState();

        validatePropertiesAreUpdatable(newResourceModel, prevResourceModel);

        if (isDynamicThingGroup(checkForThingGroup(newResourceModel.getThingGroupName(), proxyClient, OPERATION))) {
            return ProgressEvent.progress(newResourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate(CALL_GRAPH, proxyClient, newResourceModel, callbackContext)
                                    .translateToServiceRequest(Translator::translateToFirstDynamicThingGroupUpdateRequest)
                                    .makeServiceCall(this::updateDynamicThingGroupResource)
                                    .progress())
                    .then(progress -> updateResourceTags(proxy, proxyClient, progress, request, newResourceModel))
                    .then(progress -> ProgressEvent.defaultSuccessHandler(newResourceModel));
        } else {
            return ProgressEvent.progress(newResourceModel, callbackContext)
                    .then(progress ->
                            proxy.initiate(CALL_GRAPH, proxyClient, newResourceModel, callbackContext)
                                    .translateToServiceRequest(Translator::translateToUpdateThingGroupRequest)
                                    .makeServiceCall(this::updateThingGroupResource)
                                    .progress())
                    .then(progress -> updateResourceTags(proxy, proxyClient, progress, request, newResourceModel))
                    .then(progress -> ProgressEvent.defaultSuccessHandler(newResourceModel));
        }
    }

    private void validatePropertiesAreUpdatable(ResourceModel newResourceModel, ResourceModel prevResourceModel) {
        if (!StringUtils.equals(newResourceModel.getThingGroupName(), prevResourceModel.getThingGroupName())) {
            throwCfnNotUpdatableException("ThingGroupName");
        } else if (StringUtils.isNotEmpty(newResourceModel.getArn()) &&
                !StringUtils.equals(newResourceModel.getArn(), prevResourceModel.getArn())) {
            throwCfnNotUpdatableException("Arn");
        }

        // check the case for switching between dynamic to static thing group and vice-versa
        if ((StringUtils.isNotEmpty(prevResourceModel.getQueryString()) && StringUtils.isEmpty(newResourceModel.getQueryString()))
                || (StringUtils.isEmpty(prevResourceModel.getQueryString()) && StringUtils.isNotEmpty(newResourceModel.getQueryString()))) {
            throw new CfnNotUpdatableException(InvalidRequestException.builder()
                    .message(String.format("Parameter '%s' is not updatable.", "QueryString"))
                    .build());
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidRequestException.builder()
                .message(String.format("Parameter '%s' can only be updated and not removed/added", propertyName))
                .build());
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param updateThingGroupRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdateThingGroupResponse updateThingGroupResource(
            final UpdateThingGroupRequest updateThingGroupRequest,
            final ProxyClient<IotClient> proxyClient) {
        try {
            UpdateThingGroupResponse updateThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updateThingGroupRequest, proxyClient.client()::updateThingGroup);
            logger.log(String.format("%s [%s] has successfully been updated.",
                    ResourceModel.TYPE_NAME, updateThingGroupRequest.thingGroupName()));
            return updateThingGroupResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updateThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param updateDynamicThingGroupRequest the aws service request to update a resource
     * @param proxyClient the aws service client to make the call
     * @return update resource response
     */
    private UpdateDynamicThingGroupResponse updateDynamicThingGroupResource(
            final UpdateDynamicThingGroupRequest updateDynamicThingGroupRequest,
            final ProxyClient<IotClient> proxyClient) {
        try {
            UpdateDynamicThingGroupResponse updateDynamicThingGroupResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updateDynamicThingGroupRequest, proxyClient.client()::updateDynamicThingGroup);
            logger.log(String.format("%s [%s] has successfully been updated.",
                    ResourceModel.TYPE_NAME, updateDynamicThingGroupRequest.thingGroupName()));
            return updateDynamicThingGroupResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updateDynamicThingGroupRequest.thingGroupName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation to update resource tags through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param proxy
     * @param proxyClient
     * @param progress
     * @param request
     * @param newResourceModel
     * @return
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateResourceTags(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<IotClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceHandlerRequest<ResourceModel> request, ResourceModel newResourceModel) {
        return proxy.initiate(CALL_GRAPH_TAG, proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getRequest, proxyInvocation) -> {
                    try {
                        DescribeThingGroupResponse describeThingGroupResponse = proxyInvocation.injectCredentialsAndInvokeV2(getRequest,
                                proxyInvocation.client()::describeThingGroup);

                        final String resourceArn = describeThingGroupResponse.thingGroupArn();
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
                        throw Translator.translateIotExceptionToHandlerException(getRequest.thingGroupName(), OPERATION, e);
                    }
                })
                .progress();
    }
}
