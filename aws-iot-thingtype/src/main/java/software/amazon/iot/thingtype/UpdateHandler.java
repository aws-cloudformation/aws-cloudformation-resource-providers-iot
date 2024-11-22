package software.amazon.iot.thingtype;

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.UpdateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.UpdateThingTypeResponse;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The handler updates the THING TYPE resource (if it exists) - the following attributes can be updated:
 * 1. ThingTypeProperties - only Mqtt5Configuration can be updated
 * 2. Thing Type can be deprecated/un-deprecated
 * <p>
 * API Calls for UpdateHandler:
 * UpdateThingType: To update certain ThingType properties - Mqtt5Configuration
 * DeprecateThingType: To deprecate/un-deprecate a ThingType
 * DescribeThingType: To retrieve ARN of the ThingType to make Tag and UnTag API calls
 * ListTagsForResource: To retrieve old tags associated with ThingType
 * UntagResource: To remove old tags
 * TagResource: To add new tags
 */
public class UpdateHandler extends BaseHandlerStd {

    private static final String OPERATION = "UpdateThingType";
    private static final String CALL_GRAPH = "AWS-IoT-ThingType::Update";
    private static final String CALL_GRAPH_TAG = "AWS-IoT-ThingType::Tagging";
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

        boolean deprecateActionNeeded = isDeprecateActionNeeded(newResourceModel, prevResourceModel);
        return ProgressEvent.progress(newResourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newResourceModel, callbackContext)
                                .translateToServiceRequest((resourceModel) -> Translator.translateToUpdateThingTypeRequest(newResourceModel))
                                .makeServiceCall(this::performUpdateThingType)
                                .progress())
                .then(progress -> (deprecateActionNeeded) ? performDeprecateAction(proxy, proxyClient, progress, request, newResourceModel) :  progress)
                .then(progress -> updateResourceTags(proxy, proxyClient, progress, request, newResourceModel))
                .then(progress -> ProgressEvent.defaultSuccessHandler(newResourceModel));
    }
    /**
     * Only the following properties of the ThingType resource are update-able:
     * 1. Mqtt5Configuration
     * 2. deprecateThingType
     * 3. Tags
     */
    private void validatePropertiesAreUpdatable(ResourceModel newResourceModel, ResourceModel prevResourceModel) {
        validatePropertyEquality(newResourceModel.getThingTypeName(), prevResourceModel.getThingTypeName(), "ThingTypeName");
        validateArnIfPresent(newResourceModel.getArn(), prevResourceModel.getArn());
        validateThingTypeProperties(newResourceModel.getThingTypeProperties(), prevResourceModel.getThingTypeProperties());
    }

    private void validatePropertyEquality(String newValue, String prevValue, String propertyName) {
        if (!StringUtils.equals(newValue, prevValue)) {
            throwCfnNotUpdatableException(propertyName);
        }
    }

    private void validateArnIfPresent(String newArn, String prevArn) {
        if (StringUtils.isNotEmpty(newArn) && !StringUtils.equals(newArn, prevArn)) {
            throwCfnNotUpdatableException("Arn");
        }
    }

    private void validateThingTypeProperties(
            software.amazon.iot.thingtype.ThingTypeProperties newProps,
            software.amazon.iot.thingtype.ThingTypeProperties prevProps) {
        if (Objects.isNull(newProps) != Objects.isNull(prevProps)) {
            throwCfnNotUpdatableException("ThingTypeProperties");
        }

        if (Objects.nonNull(newProps) && Objects.nonNull(prevProps)) {
            validatePropertyEquality(
                    newProps.getThingTypeDescription(),
                    prevProps.getThingTypeDescription(),
                    "ThingTypeDescription"
            );
            validateSearchableAttributes(newProps.getSearchableAttributes(), prevProps.getSearchableAttributes());
        }
    }

    private void validateSearchableAttributes(List<String> newAttrs, List<String> prevAttrs) {
        if (!areSearchableAttributesEqual(newAttrs, prevAttrs)) {
            throwCfnNotUpdatableException("SearchableAttributes");
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidRequestException.builder()
                .message(String.format("Parameter '%s' cannot be added/updated/removed", propertyName))
                .build());
    }

    private boolean areSearchableAttributesEqual(List<String> newAttrs, List<String> prevAttrs) {
        if (Objects.isNull(newAttrs) != Objects.isNull(prevAttrs)) {
            return false;
        }
        return Objects.isNull(newAttrs) || new HashSet<>(newAttrs).equals(new HashSet<>(prevAttrs));
    }

    private boolean isDeprecateActionNeeded(ResourceModel newResourceModel, ResourceModel prevResourceModel) {
        return !Objects.equals(
                newResourceModel.getDeprecateThingType(),
                prevResourceModel.getDeprecateThingType()
        );
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     */
    private Object performUpdateThingType(UpdateThingTypeRequest updateThingTypeRequest,
                                          ProxyClient<IotClient> proxyClient) {
        try {
            UpdateThingTypeResponse updateThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                    updateThingTypeRequest, proxyClient.client()::updateThingType);
            logger.log(String.format("%s [%s] has successfully been updated.",
                    ResourceModel.TYPE_NAME, updateThingTypeRequest.thingTypeName()));
            return updateThingTypeResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(updateThingTypeRequest.thingTypeName(), OPERATION, e);
        }
    }


    /**
     * Implement client invocation of the deprecate request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     */
    private ProgressEvent<ResourceModel, CallbackContext> performDeprecateAction(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<IotClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceHandlerRequest<ResourceModel> request,
            final ResourceModel newResourceModel) {
        return proxy.initiate(CALL_GRAPH_TAG, proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest((resourceModel) -> Translator.translateToDeprecateRequest(newResourceModel, false))
                .makeServiceCall((deprecateThingTypeRequest, proxyInvocation) -> {
                    try {
                        DeprecateThingTypeResponse deprecateThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                                deprecateThingTypeRequest, proxyClient.client()::deprecateThingType);
                        logger.log(String.format("Deprecate action has been successfully performed on %s [%s]",
                                ResourceModel.TYPE_NAME, deprecateThingTypeRequest.thingTypeName()));
                        return deprecateThingTypeResponse;
                    } catch (final IotException e) {
                        throw Translator.translateIotExceptionToHandlerException(deprecateThingTypeRequest.thingTypeName(), OPERATION, e);
                    }
                })
                .progress();
    }

    /**
     * Implement client invocation to update resource tags through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateResourceTags(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<IotClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceHandlerRequest<ResourceModel> request,
            final ResourceModel newResourceModel) {
        return proxy.initiate(CALL_GRAPH_TAG, proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getRequest, proxyInvocation) -> {
                    try {
                        DescribeThingTypeResponse describeThingTypeResponse = proxyInvocation.injectCredentialsAndInvokeV2(getRequest,
                                proxyInvocation.client()::describeThingType);

                        final String resourceArn = describeThingTypeResponse.thingTypeArn();
                        final Set<Tag> previousTags = new HashSet<>(listTags(proxyClient, resourceArn));
                        final Set<Tag> desiredTags = Translator.translateTagsToSdk(request.getDesiredResourceTags());

                        final Set<Tag> tagsToRemove = Sets.difference(previousTags, desiredTags);
                        final Set<Tag> tagsToAdd = Sets.difference(desiredTags, previousTags);

                        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(tagsToRemove)) {
                            proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.untagResourceRequest(resourceArn, tagsToRemove),
                                    proxyClient.client()::untagResource
                            );
                            logger.log(String.format("%s [%s] untagResourceRequest successfully completed.",
                                    ResourceModel.TYPE_NAME, resourceArn));
                        }
                        if (CollectionUtils.isNotEmpty(tagsToAdd)) {
                            proxyClient.injectCredentialsAndInvokeV2(
                                    Translator.tagResourceRequest(resourceArn, tagsToAdd),
                                    proxyClient.client()::tagResource
                            );
                            logger.log(String.format("%s [%s] tagResourceRequest successfully completed.",
                                    ResourceModel.TYPE_NAME, resourceArn));
                        }
                        return ProgressEvent.progress(progress.getResourceModel(), progress.getCallbackContext());
                    } catch (IotException e) {
                        throw Translator.translateIotExceptionToHandlerException(getRequest.thingTypeName(), OPERATION, e);
                    }
                })
                .progress();
    }
}
