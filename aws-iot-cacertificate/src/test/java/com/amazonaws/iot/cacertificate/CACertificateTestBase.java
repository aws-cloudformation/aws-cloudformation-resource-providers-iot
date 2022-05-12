package com.amazonaws.iot.cacertificate;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.mockito.Mockito.mock;

public class CACertificateTestBase {
    protected final static String CA_CERT_ID = "certificateId";
    protected final static String CA_CERT_ARN = "arn:aws:iot:us-east-1:1234567890:cacert/certificateId";
    protected final static String VERIFICATION_CERT_PEM = "VERIFICATION_PEM";
    protected final static String CA_CERT_PEM = "CA_PEM";
    protected final static String CA_CERT_STATUS_ACTIVE = "ACTIVE";
    protected final static String CA_CERT_STATUS_INACTIVE = "INACTIVE";
    protected final static String CA_CERT_AUTO_REGISTRATION_ENABLE = "ENABLE";
    protected final static String CA_CERT_AUTO_REGISTRATION_DISABLE = "DISABLE";
    protected final static String REQUEST_TOKEN = "RequestToken";
    protected final static String ROLE_ARN = "arn:aws:iam::123456789012:role/light_bulb_role_001";
    protected final static String UPDATE_ROLE_ARN = "arn:aws:iam::123456789012:role/light_bulb_role_002";
    protected final static String LOGICAL_ID = "MyCACertificate";
    protected final static String TEMPLATE_BODY = "{\"Resources\": {}}";

    protected static final Credentials MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    protected static final LoggerProxy LOGGER = new LoggerProxy();
    protected AmazonWebServicesClientProxy proxy;
    protected ProxyClient<IotClient> proxyClient;
    protected IotClient iotClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(LOGGER, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        iotClient = mock(IotClient.class);
        proxyClient = MOCK_PROXY(proxy, iotClient);
    }

    private static ProxyClient<IotClient> MOCK_PROXY(
            final AmazonWebServicesClientProxy proxy,
            final IotClient sdkClient) {
        return new ProxyClient<IotClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
            IterableT
            injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
                return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
            injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IotClient client() {
                return sdkClient;
            }
        };
    }

    protected ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        return ResourceModel.builder()
                .cACertificatePem(CA_CERT_PEM)
                .verificationCertificatePem(VERIFICATION_CERT_PEM)
                .status(CA_CERT_STATUS_INACTIVE)
                .autoRegistrationStatus(CA_CERT_AUTO_REGISTRATION_DISABLE);

    }

    protected ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }
}
