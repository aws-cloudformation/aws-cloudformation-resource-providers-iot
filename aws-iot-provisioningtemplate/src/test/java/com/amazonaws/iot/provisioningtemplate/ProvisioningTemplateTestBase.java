package com.amazonaws.iot.provisioningtemplate;

import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;

public class ProvisioningTemplateTestBase {
    protected final static String TEMPLATE_NAME = "SampleTemplate";
    protected final static String TEMPLATE_DESCRIPTION = "Some template description for testing";
    protected final static String TEMPLATE_ROLE = "arn:aws:iam:us-east-1:01234567890:role/ProvisioningRole";
    protected final static String TEMPLATE_BODY = "{\"Resources\": {}}";
    protected final static String TEMPLATE_ARN = "arn:aws:iot:us-east-1:01234567890:provisioningtemplate/SampleTemplate";
    protected final static Instant CREATION_TIME = Instant.now();
    protected final static Instant UPDATE_TIME = Instant.now();
    protected final static String PAYLOAD_VERSION = "";
    protected final static String TARGET_ARN ="arn:aws:lambda:us-east-1:01234567890:function/HookFunction";

    protected final static String REQUEST_TOKEN = "REQUEST_TOKEN";
    protected final static String LOGICAL_ID = "TemplateResource";

    protected ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        final ProvisioningHook hook = ProvisioningHook.builder()
                .payloadVersion(PAYLOAD_VERSION)
                .targetArn(TARGET_ARN)
                .build();

        return ResourceModel.builder()
                .templateArn(TEMPLATE_ARN)
                .templateName(TEMPLATE_NAME)
                .description(TEMPLATE_DESCRIPTION)
                .provisioningRoleArn(TEMPLATE_ROLE)
                .templateBody(TEMPLATE_BODY)
                .preProvisioningHook(hook)
                .enabled(true);
    }

    protected ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }
}
