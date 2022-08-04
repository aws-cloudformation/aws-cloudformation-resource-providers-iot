package com.amazonaws.iot.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyResponse;
import software.amazon.awssdk.services.iot.model.GetPolicyResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.mockito.Mockito.mock;

public class PolicyTestBase {

    protected final static String REQUEST_TOKEN = "RequestToken";
    protected final static String LOGICAL_ID = "MyPolicy";
    protected final static String POLICY_NAME = "SamplePolicyName";
    protected final static String POLICY_DOCUMENT = "{ \"Version\": \"2012-10-17\", \"Statement\": [ { \"Effect\": \"Allow\", \"Action\":  \"iot:UpdateCertificate\", \"Resource\": \"*\" } ] }";
    protected final static String UPDATE_POLICY_DOCUMENT = "{ \"Version\": \"2012-10-17\", \"Statement\": [ { \"Effect\": \"Allow\", \"Action\":  [\"iot:UpdateCertificate\", \"iot:GetPolicy\"], \"Resource\": \"*\" } ] }";
    protected final static String POLICY_ARN = "arn:aws:iot:us-east-1:0123456789:policy/mypolicy";
    protected IotClient iotClient;
    protected final static GetPolicyResponse TEST_GET_POLICY_RESPONSE = getPolicyResponse();
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
                .policyName(POLICY_NAME)
                .policyDocument(convertPolicyDocumentJSONStringToMap(POLICY_DOCUMENT))
                .arn(POLICY_ARN);
    }

    protected final static CreatePolicyResponse DEFAULT_CREATE_POLICY_RESPONSE = CreatePolicyResponse.builder()
            .policyName(POLICY_NAME)
            .policyDocument(POLICY_DOCUMENT)
            .policyArn(POLICY_ARN)
            .build();

    protected static ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }

    protected final static ResourceModel DEFAULT_RESOURCE_MODEL = ResourceModel.builder()
            .policyName(POLICY_NAME)
            .arn(POLICY_ARN)
            .build();

    private static Map<String, Object> convertPolicyDocumentJSONStringToMap(final String policyDocument) {
        ObjectMapper policyDocumentMapper = new ObjectMapper();
        try {
            TypeReference<Map<String,Object>> typeRef
                    = new TypeReference<Map<String,Object>>() {};
            return policyDocumentMapper.readValue(policyDocument,  typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    protected static GetPolicyResponse getPolicyResponse() {
        return GetPolicyResponse.builder()
                .policyDocument(POLICY_DOCUMENT)
                .policyName(POLICY_NAME)
                .policyArn(POLICY_ARN)
                .build();
    }


}
