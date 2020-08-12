package com.amazonaws.iot.authorizer;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AuthorizerDescription;
import software.amazon.awssdk.services.iot.model.DescribeAuthorizerResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.amazonaws.iot.authorizer.ResourceModel.builder;
import static org.mockito.Mockito.mock;

public class AuthorizerTestBase {
    protected final static String AUTHORIZER_NAME = "SampleAuthorizer";
    protected final static String AUTHORIZER_ARN = "arn:aws:iot:us-east-1:01234567890:authorizer/SampleAuthorizer";
    protected final static String AUTHORIZER_FUNCTION_ARN = "arn:aws:lambda:us-east-1:01234567890:function/AuthorizerFunction";
    protected final static boolean SIGNING_DISABLED = false;
    protected final static String TOKEN_KEY_NAME = "MyAuthorizerToken";
    protected final static Map<String, String> TOKEN_SIGNING_PUBLIC_KEYS = createTokenSigningPublicKeys();
    protected final static String STATUS = "INACTIVE";
    protected final static ResourceModel TEST_RESOURCE_MODEL = defaultModelBuilder().build();
    protected final static DescribeAuthorizerResponse TEST_DESCRIBE_AUTHORIZER_RESPONSE = createDefaultDescribeAuthorizerResponse();

    protected static final String REQUEST_TOKEN = "RequestToken";
    protected static final String LOGICAL_ID = "Authorizer";
    protected final static ResourceHandlerRequest<ResourceModel> TEST_REQUEST = defaultRequestBuilder(TEST_RESOURCE_MODEL).build();

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

    private static Map<String, String> createTokenSigningPublicKeys() {
        final Map<String, String> keys = new HashMap<>();
        keys.put("KeyName", "SomeKeyValue");
        return keys;
    }

    protected static ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        return builder()
                .authorizerName(AUTHORIZER_NAME)
                .arn(AUTHORIZER_ARN)
                .authorizerFunctionArn(AUTHORIZER_FUNCTION_ARN)
                .signingDisabled(SIGNING_DISABLED)
                .tokenKeyName(TOKEN_KEY_NAME)
                .tokenSigningPublicKeys(TOKEN_SIGNING_PUBLIC_KEYS)
                .status(STATUS);
    }

    protected static ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }

    protected static DescribeAuthorizerResponse createDefaultDescribeAuthorizerResponse() {
        return DescribeAuthorizerResponse.builder()
                .authorizerDescription(AuthorizerDescription.builder()
                        .authorizerName(AUTHORIZER_NAME)
                        .authorizerArn(AUTHORIZER_ARN)
                        .authorizerFunctionArn(AUTHORIZER_FUNCTION_ARN)
                        .status(STATUS)
                        .signingDisabled(SIGNING_DISABLED)
                        .tokenSigningPublicKeys(TOKEN_SIGNING_PUBLIC_KEYS)
                        .tokenKeyName(TOKEN_KEY_NAME)
                        .build())
                .build();
    }
}