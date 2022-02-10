package software.amazon.iot.thingtype;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeRequest;
import software.amazon.awssdk.services.iot.model.DescribeThingTypeResponse;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Set;

/**
 * API Calls for ReadHandler:
 * DescribeThingType: To retrieve all properties of a new/updated ThingType
 * ListTagsForResource: To retrieve all tags associated with ThingType
 */
public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final DescribeThingTypeRequest describeThingTypeRequest = Translator.translateToReadRequest(resourceModel);

        try {
            DescribeThingTypeResponse describeThingTypeResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describeThingTypeRequest,
                    proxyClient.client()::describeThingType
            );
            logger.log(String.format("%s %s has successfully been read.",
                    ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName()));

            resourceModel.setArn(describeThingTypeResponse.thingTypeArn());
            resourceModel.setId(describeThingTypeResponse.thingTypeId());
            resourceModel.setThingTypeName(describeThingTypeResponse.thingTypeName());
            String thingTypeDescription = "";
            List<String> searchableAttributes = null;
            if (describeThingTypeResponse.thingTypeProperties()!=null) {
                thingTypeDescription = describeThingTypeResponse.thingTypeProperties().thingTypeDescription();
                if (describeThingTypeResponse.thingTypeProperties().searchableAttributes()!=null)
                    searchableAttributes = describeThingTypeResponse.thingTypeProperties().searchableAttributes();
            }
            resourceModel.setThingTypeProperties(ThingTypeProperties.builder()
                    .thingTypeDescription(thingTypeDescription)
                    .searchableAttributes(searchableAttributes)
                    .build());
            if (describeThingTypeResponse.thingTypeMetadata() != null &&
                    describeThingTypeResponse.thingTypeMetadata().deprecated() != null) {
                resourceModel.setDeprecateThingType(describeThingTypeResponse.thingTypeMetadata().deprecated());
            }

            final ListTagsForResourceResponse listResourceTagsResponse = proxyClient.injectCredentialsAndInvokeV2(
                    Translator.listResourceTagsRequest(resourceModel),
                    proxyClient.client()::listTagsForResource
            );
            final Set<Tag> tags = Translator.translateTagsFromSdk(listResourceTagsResponse.tags());
            logger.log(String.format("Listed Tags for %s %s.",
                    ResourceModel.TYPE_NAME, describeThingTypeRequest.thingTypeName()));
            resourceModel.setTags(tags);
        } catch (Exception e) {
            return Translator.translateExceptionToProgressEvent(resourceModel, e, logger);
        }

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }
}
