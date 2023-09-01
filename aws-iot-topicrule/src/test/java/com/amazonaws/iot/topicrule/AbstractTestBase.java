package com.amazonaws.iot.topicrule;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.TopicRule;
import software.amazon.awssdk.services.iot.model.TopicRuleListItem;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.mock;

public class AbstractTestBase {
  protected static final LoggerProxy LOGGER;
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final String TOPIC_RULE_ARN;
  protected static final String TOPIC_RULE_NAME;
  protected static final String TOPIC;
  protected static final String ROLE_ARN;
  protected static final String TOPIC_RULE_DESCRIPTION;
  protected static final String SQL_QUERY;
  protected static final String SQL_VERSION;
  protected final static String REQUEST_TOKEN;
  protected final static String LOGICAL_ID;
  protected static final LambdaAction LAMBDA_ACTION;
  protected static final RepublishAction REPUBLISH_ACTION;
  protected static final Action ACTION_FOR_LAMBDA;
  protected static final Action ACTION_FOR_REPUBLISH;
  protected static final Action ERROR_ACTION;
  protected static final List<Action> ACTION_LIST_FOR_LAMBDA;
  protected static final List<Action> ACTION_LIST_FOR_REPUBLISH;
  protected static final TopicRuleListItem TOPIC_RULE_LIST_ITEM;
  protected static final List<TopicRuleListItem> TEST_TOPIC_RULE_ITEMS;
  protected static final TopicRule TOPIC_RULE;
  protected static final ResourceModel DESIRED_TEST_RESOURCE_MODEL;
  protected static final ResourceModel PREV_TEST_RESOURCE_MODEL;
  protected static final TopicRulePayload TOPIC_RULE_PAYLOAD_LAMBDA;
  protected static final TopicRulePayload TOPIC_RULE_PAYLOAD_REPUB;
  protected static final CallbackContext TEST_CALLBACK;
  protected static final List<Tag> DESIRED_RULE_TAGS;
  protected static final List<Tag> PREV_RULE_TAGS;
  protected static final ResourceHandlerRequest<ResourceModel> TEST_REQUEST;
  protected static final software.amazon.awssdk.services.iot.model.LambdaAction SDK_LAMBDA_ACTION;
  protected static final software.amazon.awssdk.services.iot.model.Action SDK_ACTION_FOR_LAMBDA;
  protected static final software.amazon.awssdk.services.iot.model.Action SDK_ERROR_ACTION;
  protected static final List<software.amazon.awssdk.services.iot.model.Action> SDK_ACTION_LIST_FOR_LAMBDA;

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    TOPIC_RULE_NAME = "topicRuleName";
    TOPIC_RULE_ARN = "ruleArn";
    TOPIC = "topic";
    ROLE_ARN = "roleArn";
    SQL_QUERY = "sql";
    SQL_VERSION = "sql-1.1";
    TOPIC_RULE_DESCRIPTION = "description";
    REQUEST_TOKEN = "REQUEST_TOKEN";
    LOGICAL_ID = "TestTopicRule";
    LAMBDA_ACTION = LambdaAction.builder().functionArn("funcArn").build();
    REPUBLISH_ACTION = RepublishAction.builder().qos(1).roleArn(ROLE_ARN).topic(TOPIC).build();
    SDK_LAMBDA_ACTION = software.amazon.awssdk.services.iot.model.LambdaAction.builder().functionArn("funcArn").build();
    ACTION_FOR_LAMBDA = Action.builder().lambda(LAMBDA_ACTION).build();
    ACTION_FOR_REPUBLISH = Action.builder().republish(REPUBLISH_ACTION).build();
    DESIRED_RULE_TAGS = Collections.singletonList(Tag.builder().key("KEY1").value("VALUE1").build());
    PREV_RULE_TAGS = Collections.singletonList(Tag.builder().key("KEY0").value("VALUE0").build());
    ERROR_ACTION = ACTION_FOR_LAMBDA;

    SDK_ACTION_FOR_LAMBDA = software.amazon.awssdk.services.iot.model.Action.builder().lambda(SDK_LAMBDA_ACTION).build();
    SDK_ERROR_ACTION = SDK_ACTION_FOR_LAMBDA;
    ACTION_LIST_FOR_LAMBDA = Collections.singletonList(ACTION_FOR_LAMBDA);
    ACTION_LIST_FOR_REPUBLISH = Collections.singletonList(ACTION_FOR_REPUBLISH);

    SDK_ACTION_LIST_FOR_LAMBDA = Collections.singletonList(SDK_ACTION_FOR_LAMBDA);

    TOPIC_RULE_PAYLOAD_LAMBDA = TopicRulePayload.builder()
            .ruleDisabled(false)
            .awsIotSqlVersion(SQL_VERSION)
            .description(TOPIC_RULE_DESCRIPTION)
            .sql(SQL_QUERY)
            .actions(ACTION_LIST_FOR_LAMBDA)
            .errorAction(ERROR_ACTION)
            .build();
    TOPIC_RULE_PAYLOAD_REPUB = TopicRulePayload.builder()
            .ruleDisabled(true)
            .awsIotSqlVersion(SQL_VERSION)
            .description(TOPIC_RULE_DESCRIPTION)
            .sql(SQL_QUERY)
            .actions(ACTION_LIST_FOR_REPUBLISH)
            .errorAction(ERROR_ACTION)
            .build();
    DESIRED_TEST_RESOURCE_MODEL = ResourceModel.builder()
            .ruleName(TOPIC_RULE_NAME)
            .arn(TOPIC_RULE_ARN)
            .tags(DESIRED_RULE_TAGS)
            .topicRulePayload(TOPIC_RULE_PAYLOAD_LAMBDA)
            .build();
    PREV_TEST_RESOURCE_MODEL = ResourceModel.builder()
            .ruleName(TOPIC_RULE_NAME)
            .arn(TOPIC_RULE_ARN)
            .tags(PREV_RULE_TAGS)
            .topicRulePayload(TOPIC_RULE_PAYLOAD_REPUB)
            .build();
    TOPIC_RULE_LIST_ITEM = TopicRuleListItem.builder()
            .ruleName(TOPIC_RULE_NAME)
            .ruleArn(TOPIC_RULE_ARN)
            .ruleDisabled(false)
            .topicPattern("")
            .createdAt(Instant.ofEpochSecond(123456789))
            .build();
    TEST_TOPIC_RULE_ITEMS = Collections.singletonList(TOPIC_RULE_LIST_ITEM);
    TOPIC_RULE = TopicRule.builder()
            .ruleName(TOPIC_RULE_NAME)
            .ruleDisabled(false)
            .awsIotSqlVersion(SQL_VERSION)
            .description(TOPIC_RULE_DESCRIPTION)
            .sql(SQL_QUERY)
            .actions(SDK_ACTION_FOR_LAMBDA)
            .errorAction(SDK_ERROR_ACTION)
            .build();

    TEST_CALLBACK = new CallbackContext();
    TEST_REQUEST = ResourceHandlerRequest.<ResourceModel>builder()
            .clientRequestToken(REQUEST_TOKEN)
            .logicalResourceIdentifier(LOGICAL_ID)
            .desiredResourceState(DESIRED_TEST_RESOURCE_MODEL)
            .previousResourceState(PREV_TEST_RESOURCE_MODEL)
            .desiredResourceTags(DESIRED_RULE_TAGS.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue)))
            .previousResourceTags(PREV_RULE_TAGS.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue)))
            .build();

    LOGGER = new LoggerProxy();
  }

  static ProxyClient<IotClient> MOCK_PROXY(
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

  protected AmazonWebServicesClientProxy proxy;

  protected ProxyClient<IotClient> proxyClient;

  protected IotClient iotClient;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(LOGGER, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    iotClient = mock(IotClient.class);
    proxyClient = MOCK_PROXY(proxy, iotClient);
  }

  protected void assertionOnResourceModels(ResourceModel actual, ResourceModel expected) {
    assertThat(actual.getPrimaryIdentifier(), samePropertyValuesAs(expected.getPrimaryIdentifier()));
    assertThat(actual.getTags(), equalTo(expected.getTags()));
    assertThat(actual.getRuleName(), equalTo(expected.getRuleName()));
    assertThat(actual.getArn(), equalTo(expected.getArn()));
    assertThat(actual.getTopicRulePayload(), equalTo(expected.getTopicRulePayload()));
  }
}
