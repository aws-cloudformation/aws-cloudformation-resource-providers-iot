package com.amazonaws.iot.domainconfiguration;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AuthorizerConfig;
import software.amazon.awssdk.services.iot.model.CreateDomainConfigurationResponse;
import software.amazon.awssdk.services.iot.model.DescribeDomainConfigurationResponse;
import software.amazon.awssdk.services.iot.model.DomainConfigurationStatus;
import software.amazon.awssdk.services.iot.model.DomainType;
import software.amazon.awssdk.services.iot.model.ServerCertificateSummary;
import software.amazon.awssdk.services.iot.model.ServiceType;
import software.amazon.awssdk.services.iot.model.UpdateDomainConfigurationResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class DomainConfigurationTestBase {
    protected final static String DOMAIN_CONFIG_NAME = "SampleDomainConfig";
    protected final static String DOMAIN_CONFIG_NAME_GENERATED = "GeneratedDomainConfig";
    protected final static String DOMAIN_CONFIG_NAME_UPDATED = "SampleDomainConfigUpdated";
    protected final static String DOMAIN_CONFIG_ARN = "arn:aws:iot:us-east-1:0123456789:domainconfiguration/SampleDomain/2e8nm";
    protected final static String DOMAIN_NAME = "example.com";

    protected final static String DEFAULT_AUTHORIZER_NAME = "authorizer";
    protected final static AuthorizerConfig AUTHORIZER_CONFIG = AuthorizerConfig.builder()
            .defaultAuthorizerName(DEFAULT_AUTHORIZER_NAME)
            .allowAuthorizerOverride(false)
            .build();

    protected final static String SERVER_CERT_ARN = "arn:aws:iam::01234567890:certificate/Something";
    protected final static String CERT_STATUS = "VALID";
    protected final static String CERT_DETAIL = "Details";
    protected final static ServerCertificateSummary SERVER_CERTIFICATE_SUMMARY = ServerCertificateSummary.builder()
            .serverCertificateArn(SERVER_CERT_ARN)
            .serverCertificateStatus(CERT_STATUS)
            .serverCertificateStatusDetail(CERT_DETAIL)
            .build();

    protected final static DescribeDomainConfigurationResponse DEFAULT_DESCRIBE_DOMAIN_CONFIGURATION_RESPONSE = DescribeDomainConfigurationResponse.builder()
            .domainConfigurationName(DOMAIN_CONFIG_NAME)
            .domainConfigurationArn(DOMAIN_CONFIG_ARN)
            .domainName(DOMAIN_NAME)
            .authorizerConfig(AUTHORIZER_CONFIG)
            .domainConfigurationStatus("ENABLED")
            .serverCertificates(SERVER_CERTIFICATE_SUMMARY)
            .domainType(DomainType.ENDPOINT)
            .serviceType(ServiceType.CREDENTIAL_PROVIDER)
            .build();

    protected final static CreateDomainConfigurationResponse DEFAULT_CREATE_DOMAIN_CONFIGURATION_RESPONSE = CreateDomainConfigurationResponse.builder()
            .domainConfigurationName(DOMAIN_CONFIG_NAME)
            .domainConfigurationArn(DOMAIN_CONFIG_ARN)
            .build();

    protected final static CreateDomainConfigurationResponse GENERATED_CREATE_DOMAIN_CONFIGURATION_RESPONSE = CreateDomainConfigurationResponse.builder()
            .domainConfigurationName(DOMAIN_CONFIG_NAME_GENERATED)
            .domainConfigurationArn(DOMAIN_CONFIG_ARN)
            .build();

    protected final static ResourceModel DEFAULT_RESOURCE_MODEL = ResourceModel.builder()
            .domainConfigurationName(DOMAIN_CONFIG_NAME)
            .domainConfigurationArn(DOMAIN_CONFIG_ARN)
            .build();

    protected final static UpdateDomainConfigurationResponse DEFAULT_UPDATE_DOMAIN_CONFIGURATION_RESPONSE = UpdateDomainConfigurationResponse.builder()
            .domainConfigurationName(DOMAIN_CONFIG_NAME)
            .domainConfigurationArn(DOMAIN_CONFIG_ARN)
            .build();

    protected final static String REQUEST_TOKEN = "RequestToken";
    protected final static String LOGICAL_ID = "LogicalResourceId";

    protected IotClient iotClient;

    @BeforeEach
    public void setup() {
        iotClient = mock(IotClient.class);
    }

    protected ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        return ResourceModel.builder()
                .domainName(DOMAIN_NAME)
                .domainConfigurationName(DOMAIN_CONFIG_NAME)
                .domainConfigurationArn(DOMAIN_CONFIG_ARN)
                .serverCertificates(Collections.singletonList(com.amazonaws.iot.domainconfiguration.ServerCertificateSummary.builder()
                        .serverCertificateArn(SERVER_CERT_ARN)
                        .serverCertificateStatus(CERT_STATUS)
                        .serverCertificateStatusDetail(CERT_DETAIL)
                        .build()))
                .authorizerConfig(com.amazonaws.iot.domainconfiguration.AuthorizerConfig.builder()
                        .allowAuthorizerOverride(false)
                        .defaultAuthorizerName(DEFAULT_AUTHORIZER_NAME)
                        .build())
                .serviceType(ServiceType.CREDENTIAL_PROVIDER.toString())
                .domainType(DomainType.ENDPOINT.toString())
                .domainConfigurationStatus(DomainConfigurationStatus.ENABLED.toString());
    }

    protected ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }
}
