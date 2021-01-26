package com.amazonaws.iot.dimension;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateDimensionRequest;
import software.amazon.awssdk.services.iot.model.CreateDimensionResponse;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Map;

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
        } catch (ResourceAlreadyExistsException e) {
            logger.log(String.format("Resource already exists %s.", model.getName()));
            throw new CfnAlreadyExistsException(e);
        } catch (RuntimeException e) {
            return Translator.translateExceptionToErrorCode(model, e, logger);
        }

        model.setArn(createDimensionResponse.arn());
        logger.log(String.format("Created %s.", createDimensionResponse.arn()));

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private CreateDimensionRequest translateToCreateRequest(ResourceHandlerRequest<ResourceModel> request) {

        ResourceModel model = request.getDesiredResourceState();

        // Like most services, we don't require an explicit resource name in the template,
        // and, if it's not provided, generate one based on the stack ID and logical ID.
        if (StringUtils.isBlank(model.getName())) {
            model.setName(IdentifierUtils.generateResourceIdentifier(
                    request.getStackId(), request.getLogicalResourceIdentifier(),
                    request.getClientRequestToken(), GENERATED_NAME_MAX_LENGTH));
        }

        // getDesiredResourceTags combines the model and stack-level tags.
        // Reference: https://tinyurl.com/yyxtd7w6
        Map<String, String> tags = request.getDesiredResourceTags();
        // TODO: uncomment this after we update the service to allow these (only from CFN)
        // SystemTags are the default stack-level tags with aws:cloudformation prefix
        // tags.putAll(request.getSystemTags());

        // Note that the handlers act as pass-through in terms of input validation.
        // We have some validations in the json model, but we delegate deeper checks to the service.
        // If there's invalid input, we'll translate the service's InvalidRequestException,
        // keeping the readable message.
        return CreateDimensionRequest.builder()
                .name(model.getName())
                .type(model.getType())
                .stringValues(model.getStringValues())
                .tags(Translator.translateTagsToSdk(tags))
                // Note: using CFN's token here. Motivation: suppose CFN calls this handler, create call succeeds,
                // but the handler dies right before returning success. Then CFN retries. The retry will contain the
                // same token. If we don't set the clientRequestToken, the Create
                // API would throw AlreadyExistsException because the token would be different.
                .clientRequestToken(request.getClientRequestToken())
                .build();
    }
}
