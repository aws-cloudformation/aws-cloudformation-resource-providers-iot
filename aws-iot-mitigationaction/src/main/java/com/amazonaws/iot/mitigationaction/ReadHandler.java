package com.amazonaws.iot.mitigationaction;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeMitigationActionRequest;
import software.amazon.awssdk.services.iot.model.DescribeMitigationActionResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.Set;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private final IotClient iotClient;

    public ReadHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        DescribeMitigationActionRequest describeRequest = DescribeMitigationActionRequest.builder()
                .actionName(model.getActionName())
                .build();

        DescribeMitigationActionResponse describeMitigationActionResponse;
        try {
            describeMitigationActionResponse = proxy.injectCredentialsAndInvokeV2(
                    describeRequest, iotClient::describeMitigationAction);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }

        String actionArn = describeMitigationActionResponse.actionArn();
        logger.log(String.format("Called Describe for %s.", actionArn));

        // Now call ListTagsForResource, because DescribeMitigationAction doesn't provide the tags.
        List<software.amazon.awssdk.services.iot.model.Tag> iotTags = listTags(proxy, actionArn, logger);
        logger.log(String.format("Called ListTags for %s.", actionArn));

        Set<Tag> responseTags = Translator.translateTagsToCfn(iotTags);

        logger.log(String.format("Successfully described %s.", actionArn));

        return ProgressEvent.defaultSuccessHandler(
                ResourceModel.builder()
                        .actionName(describeMitigationActionResponse.actionName())
                        .mitigationActionArn(describeMitigationActionResponse.actionArn())
                        .mitigationActionId(describeMitigationActionResponse.actionId())
                        .actionParams(Translator.translateActionParamsToCfn(describeMitigationActionResponse.actionParams()))
                        .roleArn(describeMitigationActionResponse.roleArn())
                        .tags(responseTags)
                        .build());
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    List<software.amazon.awssdk.services.iot.model.Tag> listTags(AmazonWebServicesClientProxy proxy,
                                                                 String resourceArn, Logger logger) {
        return HandlerUtils.listTags(iotClient, proxy, resourceArn, logger);
    }
}
