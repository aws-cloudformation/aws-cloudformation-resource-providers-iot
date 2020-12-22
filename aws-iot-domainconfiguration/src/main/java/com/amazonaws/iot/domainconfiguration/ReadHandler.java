package com.amazonaws.iot.domainconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationResponse;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ServerCertificateSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

public class ReadHandler extends BaseHandler<CallbackContext> {
    private static final String OPERATION = "DescribeProvisioningTemplate";

    private IotClient iotClient;

    public ReadHandler() {
        iotClient = ClientBuilder.getClient();
    }

    public ReadHandler(IotClient iotClient) {
        this.iotClient = iotClient;
    }

    /**
     * Convert the SDK's authorizer config to the type expected by CloudFormation
     * @param certs The SDK AuthorizerConfig
     * @return A converted AuthorizerConfig or null if none was present in the response
     */
    private static List<com.amazonaws.iot.domainconfiguration.ServerCertificateSummary> getServerCertificates(List<ServerCertificateSummary> certs) {
        if (certs != null) {
            return certs.stream()
                    .map(cert -> com.amazonaws.iot.domainconfiguration.ServerCertificateSummary.builder()
                            .serverCertificateArn(cert.serverCertificateArn())
                            .serverCertificateStatus(cert.serverCertificateStatusAsString())
                            .serverCertificateStatusDetail(cert.serverCertificateStatusDetail())
                            .build())
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final DescribeDomainConfigurationRequest domainRequest = DescribeDomainConfigurationRequest.builder()
                .domainConfigurationName(model.getDomainConfigurationName())
                .build();

        try {
            final DescribeDomainConfigurationResponse response = proxy.injectCredentialsAndInvokeV2(
                    domainRequest,
                    iotClient::describeDomainConfiguration);

            return ProgressEvent.defaultSuccessHandler(ResourceModel.builder()
                    .arn(response.domainConfigurationArn())
                    .domainConfigurationName(response.domainConfigurationName())
                    .domainConfigurationStatus(response.domainConfigurationStatusAsString())
                    .domainName(response.domainName())
                    .domainType(response.domainTypeAsString())
                    .serviceType(response.serviceTypeAsString())
                    .authorizerConfig(ResourceUtil.getResourceModelAuthorizerConfig(response.authorizerConfig()))
                    .serverCertificates(getServerCertificates(response.serverCertificates()))
                    .build());

        } catch (IotException e) {
            throw ExceptionTranslator.translateIotExceptionToHandlerException(e, OPERATION, model.getDomainConfigurationName());
        }
    }
}
