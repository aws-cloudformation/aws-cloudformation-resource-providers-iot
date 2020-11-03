package com.amazonaws.iot.topicruledestination;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.HttpUrlDestinationProperties;
import software.amazon.awssdk.services.iot.model.TopicRuleDestination;
import software.amazon.awssdk.services.iot.model.TopicRuleDestinationStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AbstractTestBase {
    protected static final LoggerProxy LOGGER;
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final HttpUrlDestinationSummary TEST_HTTP_DEST_PROPERTIES;
    protected static final TopicRuleDestination TEST_TOPIC_RULE_DEST;
    protected static final String TEST_CONFIRM_URL;
    protected static final String TEST_ARN;
    protected static final TopicRuleDestinationStatus TEST_STATUS;
    protected static final ResourceModel TEST_RESOURCE_MODEL;
    protected static final CallbackContext TEST_CALLBACK;
    protected static final ResourceHandlerRequest<ResourceModel> TEST_REQUEST;

    static {
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
        TEST_CONFIRM_URL = "https://foobar.com";
        TEST_ARN = "arn";
        TEST_STATUS = TopicRuleDestinationStatus.ENABLED;
        TEST_HTTP_DEST_PROPERTIES = HttpUrlDestinationSummary.builder().confirmationUrl(TEST_CONFIRM_URL).build();
        TEST_RESOURCE_MODEL = ResourceModel.builder()
                .status(TEST_STATUS.name())
                .arn(TEST_ARN)
                .httpUrlProperties(TEST_HTTP_DEST_PROPERTIES)
                .build();
        TEST_TOPIC_RULE_DEST = TopicRuleDestination.builder()
                .arn(TEST_ARN)
                .status(TEST_STATUS)
                .httpUrlProperties(HttpUrlDestinationProperties.builder().confirmationUrl(TEST_CONFIRM_URL).build())
                .build();
        TEST_CALLBACK = new CallbackContext();
        TEST_REQUEST = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(TEST_RESOURCE_MODEL)
                .build();
        LOGGER = new LoggerProxy();
    }

    static ProxyClient<IotClient> MOCK_PROXY(final AmazonWebServicesClientProxy proxy, final IotClient sdkClient) {
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

    protected AmazonWebServicesClientProxy proxy;

    protected ProxyClient<IotClient> proxyClient;

    protected IotClient iotClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(LOGGER, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        iotClient = mock(IotClient.class);
        proxyClient = MOCK_PROXY(proxy, iotClient);
    }

    protected void assertionOnResourceModels(ResourceModel actualModel, ResourceModel expectedModel) {
        assertThat(actualModel.getArn()).isEqualTo(expectedModel.getArn());
        assertThat(actualModel.getStatus()).isEqualTo(expectedModel.getStatus());
        assertThat(actualModel.getStatusReason()).isEqualTo(expectedModel.getStatusReason());
        assertThat(actualModel.getHttpUrlProperties()).isEqualTo(expectedModel.getHttpUrlProperties());
      //  assertThat(actualModel.getVpcProperties()).isEqualTo(expectedModel.getVpcProperties());
    }
}
