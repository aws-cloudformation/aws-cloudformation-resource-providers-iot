package com.amazonaws.iot.securityprofile;

import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.DescribeSecurityProfileResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
        String securityProfileName = model.getSecurityProfileName();

        DescribeSecurityProfileRequest describeRequest = DescribeSecurityProfileRequest.builder()
                .securityProfileName(securityProfileName)
                .build();
        DescribeSecurityProfileResponse describeResponse;
        try {
            describeResponse = proxy.injectCredentialsAndInvokeV2(
                    describeRequest, iotClient::describeSecurityProfile);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToProgressEvent(model, e, logger);
        }


        String securityProfileArn = describeResponse.securityProfileArn();
        logger.log("Called Describe for " + securityProfileArn);

        // DescribeSecurityProfile doesn't provide the attached targets, so we call ListTargetsForSecurityProfile.
        Set<String> targetArns = listTargetsForSecurityProfile(proxy, securityProfileName);
        logger.log("Listed targets for " + securityProfileArn);

        // DescribeSecurityProfile doesn't provide the tags, so we call ListTagsForResource.
        Set<software.amazon.awssdk.services.iot.model.Tag> iotTags = listTags(proxy, securityProfileArn);
        logger.log("Listed tags for " + securityProfileArn);

        ResourceModel resourceModel = buildResourceModel(describeResponse, targetArns, iotTags);

        return ProgressEvent.defaultSuccessHandler(resourceModel);
    }

    // This facilitates mocking in the unit tests.
    // It would be nicer to instead pass HandlerUtils (which we can mock)
    // to the constructor, but the framework requires the constructor to have 0 args.
    @VisibleForTesting
    Set<software.amazon.awssdk.services.iot.model.Tag> listTags(AmazonWebServicesClientProxy proxy,
                                                                String resourceArn) {
        return HandlerUtils.listTags(iotClient, proxy, resourceArn);
    }

    @VisibleForTesting
    Set<String> listTargetsForSecurityProfile(AmazonWebServicesClientProxy proxy,
                                              String securityProfileName) {
        return HandlerUtils.listTargetsForSecurityProfile(
                iotClient, proxy, securityProfileName);
    }

    ResourceModel buildResourceModel(
            DescribeSecurityProfileResponse describeResponse,
            Set<String> targetArns,
            Set<software.amazon.awssdk.services.iot.model.Tag> iotTags) {

        ResourceModel.ResourceModelBuilder resourceModelBuilder = ResourceModel.builder()
                .securityProfileName(describeResponse.securityProfileName())
                .securityProfileDescription(describeResponse.securityProfileDescription())
                .securityProfileArn(describeResponse.securityProfileArn())
                .targetArns(targetArns)
                .tags(Translator.translateTagsFromIotToCfn(iotTags));

        // For collections, we're using the .has* methods to differentiate between null and empty collections
        // from DescribeSecurityProfileResponse.
        // SDK converts nulls from Describe API to empty DefaultSdkAutoConstructList/Maps,
        // so if we simply translate without the .has* check, nulls will turn into empty collections.
        if (describeResponse.hasBehaviors()) {
            resourceModelBuilder.behaviors(Translator.translateBehaviorListFromIotToCfn(
                    describeResponse.behaviors()));
        }
        if (describeResponse.hasAlertTargets()) {
            resourceModelBuilder.alertTargets(Translator.translateAlertTargetMapFromIotToCfn(
                    describeResponse.alertTargetsAsStrings()));
        }
        if (describeResponse.hasAdditionalMetricsToRetainV2()) {
            resourceModelBuilder.additionalMetricsToRetainV2(
                    Translator.translateMetricToRetainListFromIotToCfn(
                            describeResponse.additionalMetricsToRetainV2()));
        }

        return resourceModelBuilder.build();
    }
}
