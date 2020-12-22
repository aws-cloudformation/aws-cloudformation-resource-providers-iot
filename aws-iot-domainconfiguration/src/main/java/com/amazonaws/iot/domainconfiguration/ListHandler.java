package com.amazonaws.iot.domainconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListDomainConfigurationsRequest;
import software.amazon.awssdk.services.iot.model.ListDomainConfigurationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ListHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "ListProvisioningTemplates";

    private IotClient iotClient;

    public ListHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public ListHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ListDomainConfigurationsRequest domainRequest = ListDomainConfigurationsRequest.builder()
                .pageSize(50)
                .marker(request.getNextToken())
                .build();

        try {
            final ListDomainConfigurationsResponse response = proxy.injectCredentialsAndInvokeV2(
                    domainRequest,
                    iotClient::listDomainConfigurations);

            final List<ResourceModel> models = response.domainConfigurations().stream()
                    .map(domainConfig-> ResourceModel.builder()
                            .domainConfigurationName(domainConfig.domainConfigurationName())
                            .arn(domainConfig.domainConfigurationArn())
                            .serviceType(domainConfig.serviceTypeAsString())
                            .build())
                    .collect(Collectors.toList());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .nextToken(response.nextMarker())
                    .status(OperationStatus.SUCCESS)
                    .build();

        } catch (IotException e) {
            throw ExceptionTranslator.translateIotExceptionToHandlerException(e, OPERATION, "");
        }
    }
}
