package software.amazon.iot.thingtype;

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DeprecateThingTypeResponse;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The handler updates the THING TYPE resource (if it exists)
 *
 * API Calls for UpdateHandler:
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

        return ProgressEvent.progress(newResourceModel, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, newResourceModel, callbackContext)
                                .translateToServiceRequest((resourceModel) -> Translator.translateToDeprecateRequest(newResourceModel, false))
                                .makeServiceCall(this::performDeprecate)
                                .progress())
                .then(progress -> updateResourceTags(proxy, proxyClient, progress, request, newResourceModel))
                .then(progress -> ProgressEvent.defaultSuccessHandler(newResourceModel));
    }

    /**
     * Only the following properties of the ThingType resource are update-able - deprecateThingType and Tags
     */
    private void validatePropertiesAreUpdatable(ResourceModel newResourceModel, ResourceModel prevResourceModel) {
        if (!StringUtils.equals(newResourceModel.getThingTypeName(), prevResourceModel.getThingTypeName())) {
            throwCfnNotUpdatableException("ThingTypeName");
        }
        if (StringUtils.isNotEmpty(newResourceModel.getArn()) &&
                !StringUtils.equals(newResourceModel.getArn(), prevResourceModel.getArn())) {
            throwCfnNotUpdatableException("Arn");
        }
        if ((newResourceModel.getThingTypeProperties() != null && prevResourceModel.getThingTypeProperties() == null) ||
                (newResourceModel.getThingTypeProperties() == null && prevResourceModel.getThingTypeProperties() != null)) {
            throwCfnNotUpdatableException("ThingTypeProperties");
        } else {
            if (newResourceModel.getThingTypeProperties() != null && prevResourceModel.getThingTypeProperties() != null) {
                if (!StringUtils.equals(newResourceModel.getThingTypeProperties().getThingTypeDescription(),
                        prevResourceModel.getThingTypeProperties().getThingTypeDescription())) {
                    throwCfnNotUpdatableException("ThingTypeDescription");
                }

                if (!areSearchableAttributesEqual(newResourceModel.getThingTypeProperties().getSearchableAttributes(),
                        prevResourceModel.getThingTypeProperties().getSearchableAttributes())) {
                    throwCfnNotUpdatableException("SearchableAttributes");
                }
            }
        }
    }

    private void throwCfnNotUpdatableException(String propertyName) {
        throw new CfnNotUpdatableException(InvalidRequestException.builder()
                .message(String.format("Parameter '%s' cannot be added/updated/removed", propertyName))
                .build());
    }

    private boolean areSearchableAttributesEqual(List<String> newSearchableAttributes, List<String> prevSearchableAttributes) {
        if ((newSearchableAttributes == null && prevSearchableAttributes != null) ||
                ((newSearchableAttributes != null && prevSearchableAttributes == null))) {
            return false;
        } else if (newSearchableAttributes != null) {
            return new HashSet<>(newSearchableAttributes).equals(new HashSet<>(prevSearchableAttributes));
        }
        return true;
    }

    /**
     * Implement client invocation of the deprecate request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     */
    private DeprecateThingTypeResponse performDeprecate(
            DeprecateThingTypeRequest deprecateThingTypeRequest,
            ProxyClient<IotClient> proxyClient) {
        try {
            DeprecateThingTypeResponse deprecateThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                    deprecateThingTypeRequest, proxyClient.client()::deprecateThingType);
            logger.log(String.format("%s [%s] has successfully been updated.",
                    ResourceModel.TYPE_NAME, deprecateThingTypeRequest.thingTypeName()));
            return deprecateThingTypeResponse;
        } catch (final IotException e) {
            throw Translator.translateIotExceptionToHandlerException(deprecateThingTypeRequest.thingTypeName(), OPERATION, e);
        }
    }

    /**
     * Implement client invocation to update resource tags through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
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
