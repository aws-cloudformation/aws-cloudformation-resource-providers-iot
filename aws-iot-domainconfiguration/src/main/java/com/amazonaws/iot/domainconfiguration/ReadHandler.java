package com.amazonaws.iot.domainconfiguration;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationRequest;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServerCertificateSummary;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
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
                    .domainConfigurationArn(response.domainConfigurationArn())
                    .domainConfigurationName(response.domainConfigurationName())
                    .domainConfigurationStatus(response.domainConfigurationStatusAsString())
                    .domainName(response.domainName())
                    .domainType(response.domainTypeAsString())
                    .serviceType(response.serviceTypeAsString())
                    .authorizerConfig(ResourceUtil.getResourceModelAuthorizerConfig(response.authorizerConfig()))
                    .serverCertificates(getServerCertificates(response.serverCertificates()))
                    .build());

        } catch (final InternalFailureException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(domainRequest.toString(), e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, domainRequest.domainConfigurationName());
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        }
    }
}
