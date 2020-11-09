package com.amazonaws.iot.dimension;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateDimensionRequest;
import software.amazon.awssdk.services.iot.model.CreateDimensionResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

public class CreateHandler extends BaseHandler<CallbackContext> {

    // Copied value from software.amazon.cloudformation.resource.IdentifierUtils
    private static final int GENERATED_NAME_MAX_LENGTH = 40;

    private final IotClient iotClient;

    public CreateHandler() {
        iotClient = IotClient.builder().build();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            Logger logger) {

        CreateDimensionRequest createRequest = translateToCreateRequest(request);

        ResourceModel model = request.getDesiredResourceState();
        if (!StringUtils.isEmpty(model.getArn())) {
            logger.log(String.format("Arn is read-only, but the caller passed %s.", model.getArn()));
            // Note: this is necessary even though Arn is marked readOnly in the schema.
            return ProgressEvent.failed(model, callbackContext, HandlerErrorCode.InvalidRequest,
                    "Arn is a read-only property and cannot be set.");
        }

        CreateDimensionResponse createDimensionResponse;
        try {
            createDimensionResponse = proxy.injectCredentialsAndInvokeV2(
                    createRequest, iotClient::createDimension);
        } catch (IotException e) {
            throw Translator.translateIotExceptionToCfn(e);
        }

        model.setArn(createDimensionResponse.arn());
        logger.log(String.format("Created %s.", createDimensionResponse.arn()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateDimensionRequest translateToCreateRequest(ResourceHandlerRequest<ResourceModel> request) {

        ResourceModel model = request.getDesiredResourceState();

        // We don't require the Name field in CFN resource definition.
        // If it's not provided, generate one based on the Logical ID + Idempotency Token
        if (StringUtils.isBlank(model.getName())) {
            model.setName(IdentifierUtils.generateResourceIdentifier(
                    request.getStackId(), request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(), GENERATED_NAME_MAX_LENGTH));
        }

        // getDesiredResourceTags combines the model and stack-level tags.
        // Reference: https://tinyurl.com/y2p8medk
        Map<String, String> tags = request.getDesiredResourceTags();
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // tags.putAll(request.getSystemTags());

        return CreateDimensionRequest.builder()
                .name(model.getName())
                .type(model.getType())
                .stringValues(model.getStringValues())
                .tags(Translator.translateTagsToSdk(tags))
                // Note: using CFN's token here. Motivation: suppose CFN calls this handler, create call succeeds,
                // but the handler dies right before returning success. Then CFN retries. The retry will contain the
                // same token. If we don't set the clientRequestToken, the Create
                // API would throw RAEE because the token would be different.
                .clientRequestToken(request.getClientRequestToken())
                .build();
    }
}
