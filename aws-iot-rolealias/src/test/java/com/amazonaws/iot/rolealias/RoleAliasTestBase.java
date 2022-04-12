package com.amazonaws.iot.rolealias;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreateRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.DescribeRoleAliasResponse;
import software.amazon.awssdk.services.iot.model.RoleAliasDescription;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.mockito.Mockito.mock;

public class RoleAliasTestBase {
    protected final static String REQUEST_TOKEN = "RequestToken";
    protected final static String LOGICAL_ID = "MyRoleAlias";
    protected final static String ROLE_ALIAS = "SampleRoleAlias";
    protected final static String ROLE_ARN = "arn:aws:iam::123456789012:role/light_bulb_role_001";
    protected final static String UPDATE_ROLE_ARN = "arn:aws:iam::123456789012:role/light_bulb_role_002";
    protected final static String ROLE_ALIAS_ARN = "arn:aws:iot:us-west-2:123456789012:rolealias/SampleRoleAlias";
    protected final static Integer CREDENTIAL_DURATION_SECONDS = 3600;
    protected IotClient iotClient;
    protected final static DescribeRoleAliasResponse TEST_DESCRIBE_ROLE_ALIAS_RESPONSE = getRoleAliasResponse();
    protected static final Credentials MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    protected static final LoggerProxy LOGGER = new LoggerProxy();

    protected AmazonWebServicesClientProxy proxy;
    protected ProxyClient<IotClient> proxyClient;

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

    protected static ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        return ResourceModel.builder()
                .roleAlias(ROLE_ALIAS)
                .roleAliasArn(ROLE_ALIAS_ARN)
                .roleArn(ROLE_ARN)
                .credentialDurationSeconds(CREDENTIAL_DURATION_SECONDS);
    }

    protected final static CreateRoleAliasResponse DEFAULT_CREATE_ROLE_ALIAS_RESPONSE = CreateRoleAliasResponse.builder()
            .roleAlias(ROLE_ALIAS)
            .roleAliasArn(ROLE_ALIAS_ARN)
            .build();

    protected static ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }

    protected final static ResourceModel DEFAULT_RESOURCE_MODEL = ResourceModel.builder()
            .roleAlias(ROLE_ALIAS)
            .roleAliasArn(ROLE_ALIAS_ARN)
            .roleArn(ROLE_ARN)
            .credentialDurationSeconds(CREDENTIAL_DURATION_SECONDS)
            .build();


    protected static DescribeRoleAliasResponse getRoleAliasResponse() {
        RoleAliasDescription description = RoleAliasDescription.builder()
                .credentialDurationSeconds(CREDENTIAL_DURATION_SECONDS)
                .roleAlias(ROLE_ALIAS)
                .roleAliasArn(ROLE_ALIAS_ARN)
                .roleArn(ROLE_ARN)
                .owner("123456789012")
                .creationDate(Instant.now())
                .lastModifiedDate(Instant.now())
                .build();
        return DescribeRoleAliasResponse.builder()
                .roleAliasDescription(description)
                .build();
    }

}
