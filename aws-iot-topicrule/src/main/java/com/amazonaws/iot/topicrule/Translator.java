package com.amazonaws.iot.topicrule;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.iot.model.ConflictingResourceUpdateException;
import software.amazon.awssdk.services.iot.model.CreateTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.DeleteTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.GetTopicRuleResponse;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTopicRulesRequest;
import software.amazon.awssdk.services.iot.model.ListTopicRulesResponse;
import software.amazon.awssdk.services.iot.model.ReplaceTopicRuleRequest;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.SqlParseException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.TopicRule;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

    /**
     * Request to create a resource
     * @param model resource model
     * @return awsRequest the aws service request to create a resource
     */
    static CreateTopicRuleRequest translateToCreateRequest(final ResourceModel model, final Map<String, String> desiredTags) {
        return CreateTopicRuleRequest.builder()
                .ruleName(model.getRuleName())
                .tags(getTags(desiredTags))
                .topicRulePayload(getTopicRulePayload(model.getTopicRulePayload()))
                .build();
    }

    /**
     * Request to read a resource
     * @param model resource model
     * @return awsRequest the aws service request to describe a resource
     */
    static GetTopicRuleRequest translateToReadRequest(final ResourceModel model) {
        final String ruleName = Optional.ofNullable(model.getRuleName()).orElse(model.getId());
        return GetTopicRuleRequest.builder().ruleName(ruleName).build();
    }

    /**
     * Translates resource object from sdk into a resource model
     * @param awsResponse the aws service describe resource response
     * @return model resource model
     */
    static ResourceModel translateFromReadResponse(final GetTopicRuleResponse awsResponse) {
        final TopicRule rule = awsResponse.rule();
        return ResourceModel.builder()
                .arn(awsResponse.ruleArn())
                .id(rule.ruleName())
                .ruleName(rule.ruleName())
                .topicRulePayload(TopicRulePayload.builder()
                        .ruleDisabled(rule.ruleDisabled())
                        .actions(rule.actions().stream().map(Translator::translateToResourceAction).collect(Collectors.toList()))
                        .awsIotSqlVersion(rule.awsIotSqlVersion())
                        .description(rule.description())
                        .sql(rule.sql())
                        .errorAction(translateToResourceAction(rule.errorAction()))
                        .build())
                .build();
    }

    /**
     * Request to delete a resource
     * @param model resource model
     * @return awsRequest the aws service request to delete a resource
     */
    static DeleteTopicRuleRequest translateToDeleteRequest(final ResourceModel model) {
        final String ruleName = Optional.ofNullable(model.getRuleName()).orElse(model.getId());
        return DeleteTopicRuleRequest.builder().ruleName(ruleName).build();
    }

    /**
     * Request to update properties of a previously created resource
     * @param model resource model
     * @return awsRequest the aws service request to modify a resource
     */
    static ReplaceTopicRuleRequest translateToReplaceTopicRuleRequest(final ResourceModel model) {
        final String ruleName = Optional.ofNullable(model.getRuleName()).orElse(model.getId());
        return ReplaceTopicRuleRequest.builder()
                .ruleName(ruleName)
                .topicRulePayload(getTopicRulePayload(model.getTopicRulePayload()))
                .build();
    }

    /**
     * Request to list resources
     * @param nextToken token passed to the aws service list resources request
     * @return awsRequest the aws service request to list resources within aws account
     */
    static ListTopicRulesRequest translateToListRequest(final String nextToken, final int maxResults) {
        return ListTopicRulesRequest.builder().nextToken(nextToken).maxResults(maxResults).build();
    }

    /**
     * Translates resource objects from sdk into a resource model (primary identifier only)
     * @param awsResponse the aws service describe resource response
     * @return list of resource models
     */
    static List<ResourceModel> translateFromListResponse(final ListTopicRulesResponse awsResponse) {
        return streamOfOrEmpty(awsResponse.rules())
                .map(resource -> ResourceModel.builder()
                        .arn(resource.ruleArn())
                        .id(resource.ruleName())
                        .ruleName(resource.ruleName())
                        .topicRulePayload(TopicRulePayload.builder().ruleDisabled(resource.ruleDisabled()).build())
                        .build())
                .collect(Collectors.toList());
    }

    static BaseHandlerException translateIotExceptionToHandlerException(String resourceIdentifier, String operationName, IotException e) {
        if (e instanceof ResourceAlreadyExistsException) {
            return new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, resourceIdentifier, e);
        } else if (e instanceof UnauthorizedException || e instanceof ResourceNotFoundException) {
            return new CfnNotFoundException(ResourceModel.TYPE_NAME, resourceIdentifier, e);
        } else if (e instanceof InternalFailureException) {
            return new CfnInternalFailureException(e);
        } else if (e instanceof ServiceUnavailableException) {
            return new CfnGeneralServiceException(operationName, e);
        } else if (e instanceof InvalidRequestException || e instanceof SqlParseException) {
            return new CfnInvalidRequestException(e);
        } else if (e instanceof ConflictingResourceUpdateException) {
            return new CfnResourceConflictException(ResourceModel.TYPE_NAME, resourceIdentifier, e.getMessage(), e);
        } else if (e instanceof ThrottlingException) {
            return new CfnThrottlingException(operationName, e);
        } else if (e.statusCode() == 403) {
            return new CfnAccessDeniedException(operationName, e);
        } else {
            return new CfnServiceInternalErrorException(operationName, e);
        }
    }

    static ListTagsForResourceRequest translateToListTagsRequest(String resourceArn, String token) {
        return ListTagsForResourceRequest.builder().resourceArn(resourceArn).nextToken(token).build();
    }

    static software.amazon.awssdk.services.iot.model.TopicRulePayload getTopicRulePayload(TopicRulePayload topicRulePayload) {
        return software.amazon.awssdk.services.iot.model.TopicRulePayload.builder()
                .description(topicRulePayload.getDescription())
                .actions(topicRulePayload.getActions().stream().map(Translator::translateAction).collect(Collectors.toList()))
                .awsIotSqlVersion(topicRulePayload.getAwsIotSqlVersion())
                .ruleDisabled(topicRulePayload.getRuleDisabled())
                .errorAction(translateAction(topicRulePayload.getErrorAction()))
                .sql(topicRulePayload.getSql())
                .build();
    }

    private static String getTags(Map<String, String> tags) {
        return Objects.isNull(tags) ? null : tags.entrySet().stream().map(i -> i.getKey() + "=" + i.getValue()).collect(Collectors.joining("&"));
    }

    static Set<Tag> translateTagsMapToSdk(final Map<String, String> tags) {
        return Objects.isNull(tags) ? Collections.emptySet() :
                tags.entrySet().stream()
                        .map(entry -> Tag.builder().key(entry.getKey()).value(entry.getValue()).build())
                        .collect(Collectors.toSet());
    }

    static List<com.amazonaws.iot.topicrule.Tag> translateSdkTagsToResourceTags(final List<Tag> tags) {
        return CollectionUtils.emptyIfNull(tags)
                .stream()
                .map(tag -> com.amazonaws.iot.topicrule.Tag.builder().key(tag.key()).value(tag.value()).build())
                .collect(Collectors.toList());
    }

    private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty);
    }

    private static software.amazon.awssdk.services.iot.model.Action translateAction(final Action action) {
        if (action == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.Action.builder()
                .cloudwatchAlarm(translateCloudwatchAlarmAction(action.getCloudwatchAlarm()))
                .cloudwatchLogs(translateCloudwatchLogsAction(action.getCloudwatchLogs()))
                .cloudwatchMetric(translateCloudwatchMetricAction(action.getCloudwatchMetric()))
                .dynamoDB(translateDynamoDBAction(action.getDynamoDB()))
                .dynamoDBv2(translateDynamoDBv2Action(action.getDynamoDBv2()))
                .elasticsearch(translateElasticsearchAction(action.getElasticsearch()))
                .firehose(translateFirehoseAction(action.getFirehose()))
                .http(translateHttpAction(action.getHttp()))
                .iotAnalytics(translateIotAnalyticsAction(action.getIotAnalytics()))
                .iotEvents(translateIotEventsAction(action.getIotEvents()))
                .iotSiteWise(translateIotSiteWiseAction(action.getIotSiteWise()))
                .kinesis(translateKinesisAction(action.getKinesis()))
                .lambda(translateLambdaAction(action.getLambda()))
                .republish(translateRepublishAction(action.getRepublish()))
                .s3(translateS3Action(action.getS3()))
                .sns(translateSnsAction(action.getSns()))
                .sqs(translateSqsAction(action.getSqs()))
                .stepFunctions(translateStepFunctionsAction(action.getStepFunctions()))
                .timestream(translateTimestreamAction(action.getTimestream()))
                .build();
    }

    private static Collection<software.amazon.awssdk.services.iot.model.Action> translateActionCollection(final Collection<Action> actionCollection) {
        if (actionCollection == null) {
            return null;
        }
        return actionCollection.stream().map(Translator::translateAction).collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.iot.model.CloudwatchAlarmAction translateCloudwatchAlarmAction(final CloudwatchAlarmAction cloudwatchAlarmAction) {
        if (cloudwatchAlarmAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.CloudwatchAlarmAction.builder()
                .alarmName(cloudwatchAlarmAction.getAlarmName())
                .roleArn(cloudwatchAlarmAction.getRoleArn())
                .stateReason(cloudwatchAlarmAction.getStateReason())
                .stateValue(cloudwatchAlarmAction.getStateValue()).build();
    }

    private static software.amazon.awssdk.services.iot.model.CloudwatchLogsAction translateCloudwatchLogsAction(final CloudwatchLogsAction cloudwatchLogsAction) {
        if (cloudwatchLogsAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.CloudwatchLogsAction.builder()
                .logGroupName(cloudwatchLogsAction.getLogGroupName())
                .roleArn(cloudwatchLogsAction.getRoleArn())
                .build();
    }

    private static software.amazon.awssdk.services.iot.model.TimestreamAction translateTimestreamAction(final TimestreamAction timestreamAction) {
        if (timestreamAction == null) {
            return null;
        }
        software.amazon.awssdk.services.iot.model.TimestreamAction.Builder sdkTimestreamActionBuilder = software.amazon.awssdk.services.iot.model.TimestreamAction
                .builder()
                .roleArn(timestreamAction.getRoleArn())
                .databaseName(timestreamAction.getDatabaseName())
                .tableName(timestreamAction.getTableName())
                .dimensions(timestreamAction.getDimensions().stream()
                        .map(dim -> software.amazon.awssdk.services.iot.model.TimestreamDimension.builder()
                                .name(dim.getName())
                                .value(dim.getValue())
                                .build())
                        .collect(Collectors.toList()));
        Optional.ofNullable(timestreamAction.getTimestamp())
                .ifPresent(ts -> sdkTimestreamActionBuilder.timestamp(
                        software.amazon.awssdk.services.iot.model.TimestreamTimestamp.builder().value(ts.getValue()).unit(ts.getUnit()).build()));
        return sdkTimestreamActionBuilder.build();
    }

    private static software.amazon.awssdk.services.iot.model.CloudwatchMetricAction translateCloudwatchMetricAction(final CloudwatchMetricAction cloudwatchMetricAction) {
        if (cloudwatchMetricAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.CloudwatchMetricAction.builder()
                .metricName(cloudwatchMetricAction.getMetricName())
                .metricNamespace(cloudwatchMetricAction.getMetricNamespace())
                .metricTimestamp(cloudwatchMetricAction.getMetricTimestamp())
                .metricUnit(cloudwatchMetricAction.getMetricUnit())
                .metricValue(cloudwatchMetricAction.getMetricValue())
                .roleArn(cloudwatchMetricAction.getRoleArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.DynamoDBAction translateDynamoDBAction(final DynamoDBAction dynamoDBAction) {
        if (dynamoDBAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.DynamoDBAction.builder()
                .hashKeyField(dynamoDBAction.getHashKeyField())
                .hashKeyType(dynamoDBAction.getHashKeyType())
                .hashKeyValue(dynamoDBAction.getHashKeyValue())
                .payloadField(dynamoDBAction.getPayloadField())
                .rangeKeyField(dynamoDBAction.getRangeKeyField())
                .rangeKeyType(dynamoDBAction.getRangeKeyType())
                .rangeKeyValue(dynamoDBAction.getRangeKeyValue())
                .roleArn(dynamoDBAction.getRoleArn())
                .tableName(dynamoDBAction.getTableName()).build();
    }

    private static software.amazon.awssdk.services.iot.model.DynamoDBv2Action translateDynamoDBv2Action(final DynamoDBv2Action dynamoDBv2Action) {
        if (dynamoDBv2Action == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.DynamoDBv2Action.builder()
                .putItem(translatePutItemInput(dynamoDBv2Action.getPutItem()))
                .roleArn(dynamoDBv2Action.getRoleArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.ElasticsearchAction translateElasticsearchAction(final ElasticsearchAction elasticsearchAction) {
        if (elasticsearchAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.ElasticsearchAction.builder()
                .endpoint(elasticsearchAction.getEndpoint())
                .id(elasticsearchAction.getId())
                .index(elasticsearchAction.getIndex())
                .roleArn(elasticsearchAction.getRoleArn())
                .type(elasticsearchAction.getType()).build();
    }

    private static software.amazon.awssdk.services.iot.model.FirehoseAction translateFirehoseAction(final FirehoseAction firehoseAction) {
        if (firehoseAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.FirehoseAction.builder()
                .deliveryStreamName(firehoseAction.getDeliveryStreamName())
                .separator(firehoseAction.getSeparator())
                .roleArn(firehoseAction.getRoleArn())
                .build();
    }

    private static software.amazon.awssdk.services.iot.model.HttpAction translateHttpAction(final HttpAction httpAction) {
        if (httpAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.HttpAction.builder()
                .url(httpAction.getUrl())
                .confirmationUrl(httpAction.getConfirmationUrl())
                .headers(translateHttpActionHeaderCollection(httpAction.getHeaders()))
                .auth(translateHttpAuthorization(httpAction.getAuth())).build();
    }

    private static software.amazon.awssdk.services.iot.model.IotAnalyticsAction translateIotAnalyticsAction(final IotAnalyticsAction iotAnalyticsAction) {
        if (iotAnalyticsAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.IotAnalyticsAction.builder()
                .channelName(iotAnalyticsAction.getChannelName())
                .roleArn(iotAnalyticsAction.getRoleArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.IotEventsAction translateIotEventsAction(final IotEventsAction iotEventsAction) {
        if (iotEventsAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.IotEventsAction.builder()
                .inputName(iotEventsAction.getInputName())
                .messageId(iotEventsAction.getMessageId())
                .roleArn(iotEventsAction.getRoleArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.IotSiteWiseAction translateIotSiteWiseAction(final IotSiteWiseAction iotSiteWiseAction) {
        if (iotSiteWiseAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.IotSiteWiseAction.builder()
                .putAssetPropertyValueEntries(translatePutAssetPropertyValueEntryCollection(iotSiteWiseAction.getPutAssetPropertyValueEntries()))
                .roleArn(iotSiteWiseAction.getRoleArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.KinesisAction translateKinesisAction(final KinesisAction kinesisAction) {
        if (kinesisAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.KinesisAction.builder()
                .partitionKey(kinesisAction.getPartitionKey())
                .roleArn(kinesisAction.getRoleArn())
                .streamName(kinesisAction.getStreamName()).build();
    }

    private static software.amazon.awssdk.services.iot.model.LambdaAction translateLambdaAction(final LambdaAction lambdaAction) {
        if (lambdaAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.LambdaAction.builder()
                .functionArn(lambdaAction.getFunctionArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.PutItemInput translatePutItemInput(final PutItemInput putItemInput) {
        if (putItemInput == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.PutItemInput.builder()
                .tableName(putItemInput.getTableName())
                .build();
    }

    private static software.amazon.awssdk.services.iot.model.RepublishAction translateRepublishAction(final RepublishAction republishAction) {
        if (republishAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.RepublishAction.builder()
                .roleArn(republishAction.getRoleArn())
                .topic(republishAction.getTopic())
                .qos(republishAction.getQos()).build();
    }

    private static software.amazon.awssdk.services.iot.model.S3Action translateS3Action(final S3Action s3Action) {
        if (s3Action == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.S3Action.builder()
                .bucketName(s3Action.getBucketName())
                .key(s3Action.getKey())
                .roleArn(s3Action.getRoleArn())
                .cannedAcl(s3Action.getCannedAcl())
                .build();
    }

    private static software.amazon.awssdk.services.iot.model.SnsAction translateSnsAction(final SnsAction snsAction) {
        if (snsAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.SnsAction.builder()
                .messageFormat(snsAction.getMessageFormat())
                .roleArn(snsAction.getRoleArn())
                .targetArn(snsAction.getTargetArn()).build();
    }

    private static software.amazon.awssdk.services.iot.model.SqsAction translateSqsAction(final SqsAction sqsAction) {
        if (sqsAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.SqsAction.builder()
                .queueUrl(sqsAction.getQueueUrl())
                .roleArn(sqsAction.getRoleArn())
                .useBase64(sqsAction.getUseBase64()).build();
    }

    private static software.amazon.awssdk.services.iot.model.StepFunctionsAction translateStepFunctionsAction(final StepFunctionsAction stepFunctionsAction) {
        if (stepFunctionsAction == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.StepFunctionsAction.builder()
                .executionNamePrefix(stepFunctionsAction.getExecutionNamePrefix())
                .roleArn(stepFunctionsAction.getRoleArn())
                .stateMachineName(stepFunctionsAction.getStateMachineName()).build();
    }

    private static software.amazon.awssdk.services.iot.model.PutAssetPropertyValueEntry translatePutAssetPropertyValueEntry(final PutAssetPropertyValueEntry putAssetPropertyValueEntry) {
        if (putAssetPropertyValueEntry == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.PutAssetPropertyValueEntry.builder()
                .entryId(putAssetPropertyValueEntry.getEntryId())
                .assetId(putAssetPropertyValueEntry.getAssetId())
                .propertyId(putAssetPropertyValueEntry.getPropertyId())
                .propertyAlias(putAssetPropertyValueEntry.getPropertyAlias())
                .propertyValues(translateAssetPropertyValueCollection(putAssetPropertyValueEntry.getPropertyValues())).build();
    }

    private static Collection<software.amazon.awssdk.services.iot.model.PutAssetPropertyValueEntry> translatePutAssetPropertyValueEntryCollection(
            final Collection<PutAssetPropertyValueEntry> putAssetPropertyValueEntryCollection) {
        if (putAssetPropertyValueEntryCollection == null) {
            return null;
        }
        return putAssetPropertyValueEntryCollection.stream().map(Translator::translatePutAssetPropertyValueEntry).collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.iot.model.AssetPropertyValue translateAssetPropertyValue(final AssetPropertyValue assetPropertyValue) {
        if (assetPropertyValue == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.AssetPropertyValue.builder()
                .value(translateAssetPropertyVariant(assetPropertyValue.getValue()))
                .timestamp(translateAssetPropertyTimestamp(assetPropertyValue.getTimestamp()))
                .quality(assetPropertyValue.getQuality()).build();
    }

    private static Collection<software.amazon.awssdk.services.iot.model.AssetPropertyValue> translateAssetPropertyValueCollection(final Collection<AssetPropertyValue> assetPropertyValueCollection) {
        if (assetPropertyValueCollection == null) {
            return null;
        }
        return assetPropertyValueCollection.stream().map(Translator::translateAssetPropertyValue).collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.iot.model.AssetPropertyVariant translateAssetPropertyVariant(final AssetPropertyVariant assetPropertyVariant) {
        if (assetPropertyVariant == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.AssetPropertyVariant.builder()
                .stringValue(assetPropertyVariant.getStringValue())
                .integerValue(assetPropertyVariant.getIntegerValue())
                .doubleValue(assetPropertyVariant.getDoubleValue())
                .booleanValue(assetPropertyVariant.getBooleanValue()).build();
    }

    private static software.amazon.awssdk.services.iot.model.AssetPropertyTimestamp translateAssetPropertyTimestamp(final AssetPropertyTimestamp assetPropertyTimestamp) {
        if (assetPropertyTimestamp == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.AssetPropertyTimestamp.builder()
                .timeInSeconds(assetPropertyTimestamp.getTimeInSeconds())
                .offsetInNanos(assetPropertyTimestamp.getOffsetInNanos()).build();
    }

    private static software.amazon.awssdk.services.iot.model.HttpActionHeader translateHttpActionHeader(final HttpActionHeader httpActionHeader) {
        if (httpActionHeader == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.HttpActionHeader.builder()
                .key(httpActionHeader.getKey())
                .value(httpActionHeader.getValue()).build();
    }

    private static Collection<software.amazon.awssdk.services.iot.model.HttpActionHeader> translateHttpActionHeaderCollection(final Collection<HttpActionHeader> httpActionHeaderCollection) {
        if (httpActionHeaderCollection == null) {
            return null;
        }
        return httpActionHeaderCollection.stream().map(Translator::translateHttpActionHeader).collect(Collectors.toList());
    }

    private static software.amazon.awssdk.services.iot.model.HttpAuthorization translateHttpAuthorization(final HttpAuthorization httpAuthorization) {
        if (httpAuthorization == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.HttpAuthorization.builder()
                .sigv4(translateSigV4Authorization(httpAuthorization.getSigv4())).build();
    }

    private static software.amazon.awssdk.services.iot.model.SigV4Authorization translateSigV4Authorization(final SigV4Authorization sigV4Authorization) {
        if (sigV4Authorization == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.SigV4Authorization.builder()
                .signingRegion(sigV4Authorization.getSigningRegion())
                .serviceName(sigV4Authorization.getServiceName())
                .roleArn(sigV4Authorization.getRoleArn()).build();
    }

    static Action translateToResourceAction(final software.amazon.awssdk.services.iot.model.Action action) {
        if (action == null) {
            return null;
        }
        return Action.builder()
                .cloudwatchAlarm(translateToResourceCloudwatchAlarmAction(action.cloudwatchAlarm()))
                .cloudwatchMetric(translateToResourceCloudwatchMetricAction(action.cloudwatchMetric()))
                .dynamoDB(translateToResourceDynamoDBAction(action.dynamoDB()))
                .dynamoDBv2(translateToResourceDynamoDBv2Action(action.dynamoDBv2()))
                .elasticsearch(translateToResourceElasticsearchAction(action.elasticsearch()))
                .firehose(translateToResourceFirehoseAction(action.firehose()))
                .http(translateToResourceHttpAction(action.http()))
                .iotAnalytics(translateToResourceIotAnalyticsAction(action.iotAnalytics()))
                .iotEvents(translateToResourceIotEventsAction(action.iotEvents()))
                .iotSiteWise(translateToResourceIotSiteWiseAction(action.iotSiteWise()))
                .kinesis(translateToResourceKinesisAction(action.kinesis()))
                .lambda(translateToResourceLambdaAction(action.lambda()))
                .republish(translateToResourceRepublishAction(action.republish()))
                .s3(translateToResourceS3Action(action.s3()))
                .sns(translateToResourceSnsAction(action.sns()))
                .sqs(translateToResourceSqsAction(action.sqs()))
                .cloudwatchLogs(translateToResourceCloudwatchLogsAction(action.cloudwatchLogs()))
                .stepFunctions(translateToResourceStepFunctionsAction(action.stepFunctions()))
                .timestream(translateToResourceTimestreamAction(action.timestream()))
                .build();
    }

    private static List<Action> translateToResourceActionCollection(final Collection<software.amazon.awssdk.services.iot.model.Action> actionCollection) {
        if (actionCollection == null) {
            return null;
        }
        return actionCollection.stream().map(Translator::translateToResourceAction).collect(Collectors.toList());
    }

    private static CloudwatchAlarmAction translateToResourceCloudwatchAlarmAction(final software.amazon.awssdk.services.iot.model.CloudwatchAlarmAction cloudwatchAlarmAction) {
        if (cloudwatchAlarmAction == null) {
            return null;
        }
        return CloudwatchAlarmAction.builder()
                .alarmName(cloudwatchAlarmAction.alarmName())
                .roleArn(cloudwatchAlarmAction.roleArn())
                .stateReason(cloudwatchAlarmAction.stateReason())
                .stateValue(cloudwatchAlarmAction.stateValue())
                .build();
    }

    private static CloudwatchMetricAction translateToResourceCloudwatchMetricAction(final software.amazon.awssdk.services.iot.model.CloudwatchMetricAction cloudwatchMetricAction) {
        if (cloudwatchMetricAction == null) {
            return null;
        }
        return CloudwatchMetricAction.builder()
                .metricName(cloudwatchMetricAction.metricName())
                .metricNamespace(cloudwatchMetricAction.metricNamespace())
                .metricTimestamp(cloudwatchMetricAction.metricTimestamp())
                .metricUnit(cloudwatchMetricAction.metricUnit())
                .metricValue(cloudwatchMetricAction.metricValue())
                .roleArn(cloudwatchMetricAction.roleArn())
                .build();
    }

    private static DynamoDBAction translateToResourceDynamoDBAction(final software.amazon.awssdk.services.iot.model.DynamoDBAction dynamoDBAction) {
        if (dynamoDBAction == null) {
            return null;
        }
        return DynamoDBAction.builder()
                .hashKeyField(dynamoDBAction.hashKeyField())
                .hashKeyType(dynamoDBAction.hashKeyType().name())
                .hashKeyValue(dynamoDBAction.hashKeyValue())
                .payloadField(dynamoDBAction.payloadField())
                .rangeKeyField(dynamoDBAction.rangeKeyField())
                .rangeKeyType(dynamoDBAction.rangeKeyType().name())
                .rangeKeyValue(dynamoDBAction.rangeKeyValue())
                .roleArn(dynamoDBAction.roleArn())
                .tableName(dynamoDBAction.tableName())
                .build();
    }

    private static DynamoDBv2Action translateToResourceDynamoDBv2Action(final software.amazon.awssdk.services.iot.model.DynamoDBv2Action dynamoDBv2Action) {
        if (dynamoDBv2Action == null) {
            return null;
        }
        return DynamoDBv2Action.builder()
                .putItem(translateToResourcePutItemInput(dynamoDBv2Action.putItem()))
                .roleArn(dynamoDBv2Action.roleArn())
                .build();
    }

    private static ElasticsearchAction translateToResourceElasticsearchAction(final software.amazon.awssdk.services.iot.model.ElasticsearchAction elasticsearchAction) {
        if (elasticsearchAction == null) {
            return null;
        }
        return ElasticsearchAction.builder()
                .endpoint(elasticsearchAction.endpoint())
                .id(elasticsearchAction.id())
                .index(elasticsearchAction.index())
                .roleArn(elasticsearchAction.roleArn())
                .type(elasticsearchAction.type())
                .build();
    }

    private static FirehoseAction translateToResourceFirehoseAction(final software.amazon.awssdk.services.iot.model.FirehoseAction firehoseAction) {
        if (firehoseAction == null) {
            return null;
        }
        return FirehoseAction.builder()
                .deliveryStreamName(firehoseAction.deliveryStreamName())
                .separator(firehoseAction.separator())
                .roleArn(firehoseAction.roleArn())
                .build();
    }

    private static HttpAction translateToResourceHttpAction(final software.amazon.awssdk.services.iot.model.HttpAction httpAction) {
        if (httpAction == null) {
            return null;
        }
        return HttpAction.builder()
                .url(httpAction.url())
                .confirmationUrl(httpAction.confirmationUrl())
                .headers(translateToResourceHttpActionHeaderCollection(httpAction.headers()))
                .auth(translateToResourceHttpAuthorization(httpAction.auth()))
                .build();
    }

    private static IotAnalyticsAction translateToResourceIotAnalyticsAction(final software.amazon.awssdk.services.iot.model.IotAnalyticsAction iotAnalyticsAction) {
        if (iotAnalyticsAction == null) {
            return null;
        }
        return IotAnalyticsAction.builder()
                .channelName(iotAnalyticsAction.channelName())
                .roleArn(iotAnalyticsAction.roleArn())
                .build();
    }

    private static IotEventsAction translateToResourceIotEventsAction(final software.amazon.awssdk.services.iot.model.IotEventsAction iotEventsAction) {
        if (iotEventsAction == null) {
            return null;
        }
        return IotEventsAction.builder()
                .inputName(iotEventsAction.inputName())
                .messageId(iotEventsAction.messageId())
                .roleArn(iotEventsAction.roleArn()).build();
    }

    static IotSiteWiseAction translateToResourceIotSiteWiseAction(final software.amazon.awssdk.services.iot.model.IotSiteWiseAction iotSiteWiseAction) {
        if (iotSiteWiseAction == null) {
            return null;
        }
        return IotSiteWiseAction.builder()
                .putAssetPropertyValueEntries(translateToResourcePutAssetPropertyValueCollection(iotSiteWiseAction.putAssetPropertyValueEntries()))
                .roleArn(iotSiteWiseAction.roleArn()).build();
    }

    private static List<PutAssetPropertyValueEntry> translateToResourcePutAssetPropertyValueCollection(final Collection<software.amazon.awssdk.services.iot.model.PutAssetPropertyValueEntry> putAssetPropertyValueEntryCollection) {
        if (putAssetPropertyValueEntryCollection == null) {
            return null;
        }
        return putAssetPropertyValueEntryCollection.stream()
                .map(Translator::translateToResourcePutAssetPropertyValueEntry)
                .collect(Collectors.toList());
    }

    private static KinesisAction translateToResourceKinesisAction(final software.amazon.awssdk.services.iot.model.KinesisAction kinesisAction) {
        if (kinesisAction == null) {
            return null;
        }
        return KinesisAction.builder()
                .partitionKey(kinesisAction.partitionKey())
                .roleArn(kinesisAction.roleArn())
                .streamName(kinesisAction.streamName())
                .build();
    }

    private static LambdaAction translateToResourceLambdaAction(final software.amazon.awssdk.services.iot.model.LambdaAction lambdaAction) {
        if (lambdaAction == null) {
            return null;
        }
        return LambdaAction.builder()
                .functionArn(lambdaAction.functionArn())
                .build();
    }

    private static PutItemInput translateToResourcePutItemInput(final software.amazon.awssdk.services.iot.model.PutItemInput putItemInput) {
        if (putItemInput == null) {
            return null;
        }
        return PutItemInput.builder()
                .tableName(putItemInput.tableName())
                .build();
    }

    private static RepublishAction translateToResourceRepublishAction(final software.amazon.awssdk.services.iot.model.RepublishAction republishAction) {
        if (republishAction == null) {
            return null;
        }
        return RepublishAction.builder()
                .roleArn(republishAction.roleArn())
                .topic(republishAction.topic())
                .qos(republishAction.qos())
                .build();
    }

    private static S3Action translateToResourceS3Action(final software.amazon.awssdk.services.iot.model.S3Action s3Action) {
        if (s3Action == null) {
            return null;
        }
        return S3Action.builder()
                .bucketName(s3Action.bucketName())
                .key(s3Action.key())
                .roleArn(s3Action.roleArn())
                .build();
    }

    private static SnsAction translateToResourceSnsAction(final software.amazon.awssdk.services.iot.model.SnsAction snsAction) {
        if (snsAction == null) {
            return null;
        }
        return SnsAction.builder()
                .messageFormat(snsAction.messageFormat().name())
                .roleArn(snsAction.roleArn())
                .targetArn(snsAction.targetArn()).build();
    }

    private static SqsAction translateToResourceSqsAction(final software.amazon.awssdk.services.iot.model.SqsAction sqsAction) {
        if (sqsAction == null) {
            return null;
        }
        return SqsAction.builder()
                .queueUrl(sqsAction.queueUrl())
                .roleArn(sqsAction.roleArn())
                .useBase64(sqsAction.useBase64())
                .build();
    }

    private static StepFunctionsAction translateToResourceStepFunctionsAction(final software.amazon.awssdk.services.iot.model.StepFunctionsAction stepFunctionsAction) {
        if (stepFunctionsAction == null) {
            return null;
        }
        return StepFunctionsAction.builder()
                .executionNamePrefix(stepFunctionsAction.executionNamePrefix())
                .roleArn(stepFunctionsAction.roleArn())
                .stateMachineName(stepFunctionsAction.stateMachineName())
                .build();
    }

    private static PutAssetPropertyValueEntry translateToResourcePutAssetPropertyValueEntry(final software.amazon.awssdk.services.iot.model.PutAssetPropertyValueEntry putAssetPropertyValueEntry) {
        if (putAssetPropertyValueEntry == null) {
            return null;
        }
        return PutAssetPropertyValueEntry.builder()
                .entryId(putAssetPropertyValueEntry.entryId())
                .assetId(putAssetPropertyValueEntry.assetId())
                .propertyId(putAssetPropertyValueEntry.propertyId())
                .propertyAlias(putAssetPropertyValueEntry.propertyAlias())
                .propertyValues(translateToResourceAssetPropertyValueCollection(putAssetPropertyValueEntry.propertyValues()))
                .build();
    }

    private static AssetPropertyValue translateToResourceAssetPropertyValue(final software.amazon.awssdk.services.iot.model.AssetPropertyValue assetPropertyValue) {
        if (assetPropertyValue == null) {
            return null;
        }
        return AssetPropertyValue.builder()
                .value(translateToResourceAssetPropertyVariant(assetPropertyValue.value()))
                .timestamp(translateAssetToResourcePropertyTimestamp(assetPropertyValue.timestamp()))
                .quality(assetPropertyValue.quality())
                .build();
    }

    private static List<AssetPropertyValue> translateToResourceAssetPropertyValueCollection(final Collection<software.amazon.awssdk.services.iot.model.AssetPropertyValue> assetPropertyValueCollection) {
        if (assetPropertyValueCollection == null) {
            return null;
        }
        return assetPropertyValueCollection.stream().map(Translator::translateToResourceAssetPropertyValue).collect(Collectors.toList());
    }

    private static AssetPropertyVariant translateToResourceAssetPropertyVariant(final software.amazon.awssdk.services.iot.model.AssetPropertyVariant assetPropertyVariant) {
        if (assetPropertyVariant == null) {
            return null;
        }
        return AssetPropertyVariant.builder()
                .stringValue(assetPropertyVariant.stringValue())
                .integerValue(assetPropertyVariant.integerValue())
                .doubleValue(assetPropertyVariant.doubleValue())
                .booleanValue(assetPropertyVariant.booleanValue())
                .build();
    }

    private static AssetPropertyTimestamp translateAssetToResourcePropertyTimestamp(final software.amazon.awssdk.services.iot.model.AssetPropertyTimestamp assetPropertyTimestamp) {
        if (assetPropertyTimestamp == null) {
            return null;
        }
        return AssetPropertyTimestamp.builder()
                .timeInSeconds(assetPropertyTimestamp.timeInSeconds())
                .offsetInNanos(assetPropertyTimestamp.offsetInNanos())
                .build();
    }

    private static HttpActionHeader translateHttpActionHeader(final software.amazon.awssdk.services.iot.model.HttpActionHeader httpActionHeader) {
        if (httpActionHeader == null) {
            return null;
        }
        return HttpActionHeader.builder()
                .key(httpActionHeader.key())
                .value(httpActionHeader.value())
                .build();
    }

    private static List<HttpActionHeader> translateToResourceHttpActionHeaderCollection(final Collection<software.amazon.awssdk.services.iot.model.HttpActionHeader> httpActionHeaderCollection) {
        if (httpActionHeaderCollection == null) {
            return null;
        }
        return httpActionHeaderCollection.stream().map(Translator::translateHttpActionHeader).collect(Collectors.toList());
    }

    private static HttpAuthorization translateToResourceHttpAuthorization(final software.amazon.awssdk.services.iot.model.HttpAuthorization httpAuthorization) {
        if (httpAuthorization == null) {
            return null;
        }
        return HttpAuthorization.builder()
                .sigv4(translateToResourceSigV4Authorization(httpAuthorization.sigv4()))
                .build();
    }

    private static SigV4Authorization translateToResourceSigV4Authorization(final software.amazon.awssdk.services.iot.model.SigV4Authorization sigV4Authorization) {
        if (sigV4Authorization == null) {
            return null;
        }
        return SigV4Authorization.builder()
                .signingRegion(sigV4Authorization.signingRegion())
                .serviceName(sigV4Authorization.serviceName())
                .roleArn(sigV4Authorization.roleArn())
                .build();
    }

    private static CloudwatchLogsAction translateToResourceCloudwatchLogsAction(final software.amazon.awssdk.services.iot.model.CloudwatchLogsAction cloudwatchLogsAction) {
        if (cloudwatchLogsAction == null) {
            return null;
        }
        return CloudwatchLogsAction.builder()
                .logGroupName(cloudwatchLogsAction.logGroupName())
                .roleArn(cloudwatchLogsAction.roleArn())
                .build();
    }

    private static TimestreamAction translateToResourceTimestreamAction(final software.amazon.awssdk.services.iot.model.TimestreamAction timestreamAction) {
        if (timestreamAction == null) {
            return null;
        }
        TimestreamAction.TimestreamActionBuilder timestreamActionBuilder = TimestreamAction.builder()
                .roleArn(timestreamAction.roleArn())
                .databaseName(timestreamAction.databaseName())
                .tableName(timestreamAction.tableName())
                .dimensions(timestreamAction.dimensions()
                        .stream()
                        .map(dim -> TimestreamDimension.builder().name(dim.name()).value(dim.value()).build())
                        .collect(Collectors.toList()));
        Optional.ofNullable(timestreamAction.timestamp())
                .ifPresent(ts -> timestreamActionBuilder.timestamp(TimestreamTimestamp.builder().value(ts.value()).unit(ts.unit()).build()));
        return timestreamActionBuilder.build();
    }

    public static ResourceHandlerRequest<ResourceModel> setResourceIdIfNull(ResourceHandlerRequest<ResourceModel> request, ResourceModel model) {
        ResourceHandlerRequest<ResourceModel> updatedRequest = request.toBuilder().desiredResourceState(model).build();
        return updatedRequest;
    }
}
