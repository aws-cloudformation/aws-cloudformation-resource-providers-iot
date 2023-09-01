package software.amazon.iot.softwarepackage;

import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
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

public class HandlerTestBase {
    protected static final Credentials MOCK_CREDENTIALS;
    protected static final org.slf4j.Logger delegate;
    protected static final LoggerProxy LOGGER;

    static {
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss:SSS Z");
        MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");

        delegate = LoggerFactory.getLogger("testing");

        LOGGER = new LoggerProxy();
    }

    static ProxyClient<IotClient> MOCK_PROXY(final AmazonWebServicesClientProxy proxy, final IotClient iotClient) {
        return new ProxyClient<IotClient>() {
            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
            injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
                return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
            CompletableFuture<ResponseT>
            injectCredentialsAndInvokeV2Async(
                    RequestT request,
                    Function<RequestT, CompletableFuture<ResponseT>> requestFunction
            ) {
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
            injectCredentialsAndInvokeV2InputStream(
                    RequestT requestT,
                    Function<RequestT, ResponseInputStream<ResponseT>> function
            ) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
            injectCredentialsAndInvokeV2Bytes(
                    RequestT requestT,
                    Function<RequestT, ResponseBytes<ResponseT>> function
            ) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IotClient client() {
                return iotClient;
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

    protected final static String PKG_ARN = "arn:aws:iot:us-east-1:1234567890:package/packageId";
    protected final static String PKG_NAME = "PackageName";
    protected final static String PKG_DESC = "description";
    protected final static String DEFAULT_VER_NAME = "default";
    protected final static String REQUEST_TOKEN = "REQUEST_TOKEN";
    protected final static String LOGICAL_ID = "PKG";

    protected ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(
            ResourceModel model
    ) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model)
                .previousResourceState(model);
    }
}
