package com.amazonaws.iot.mitigationaction;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class TestConstants {

    protected static final String MITIGATION_ACTION_NAME = "TestMitigationActionName";
    protected static final String MITIGATION_ACTION_NAME2 = "TestMitigationActionName2";
    protected static final String CLIENT_REQUEST_TOKEN = "b99b5ee6";
    protected static final String ACTION_ARN = "arn:aws:iot:us-east-1:123456789012:mitigationAction/TestMitigationActionName";
    protected static final String ACTION_ARN2 = "arn:aws:iot:us-east-1:123456789012:mitigationAction" +
            "/TestMitigationActionName2";
    protected static final String ACTION_ID = "12345-abced-6789-efgh";
    protected static final String MITIGATION_ACTION_ROLE_ARN = "arn:aws:iot:us-east-1:123456789012:Role/TestRole";
    protected static final Instant CREATION_DATE1 = Instant.parse("2020-11-04T10:00:00.00Z");
    protected static final Instant CREATION_DATE2 = Instant.parse("2020-11-04T11:00:00.00Z");
    protected static final ReplaceDefaultPolicyVersionParams REPLACE_DEFAULT_POLICY_VERSION_PARAMS =
            ReplaceDefaultPolicyVersionParams.builder()
            .templateName("BLANK_POLICY")
            .build();
    protected static final ActionParams ACTION_PARAMS = ActionParams.builder()
            .replaceDefaultPolicyVersionParams(REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
            .build();

    protected static final software.amazon.awssdk.services.iot.model.ReplaceDefaultPolicyVersionParams SDK_REPLACE_DEFAULT_POLICY_VERSION_PARAMS =
            software.amazon.awssdk.services.iot.model.ReplaceDefaultPolicyVersionParams.builder()
            .templateName("BLANK_POLICY")
            .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
            .replaceDefaultPolicyVersionParams(SDK_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
            .build();

    protected static final PublishFindingToSnsParams PUBLISH_FINDING_TO_SNS_PARAMS = PublishFindingToSnsParams.builder()
            .topicArn("arn:aws:sns:us-east-1:072938559174:myTopic.fifo")
            .build();

    protected static final ActionParams ACTION_PARAMS2 = ActionParams.builder()
            .publishFindingToSnsParams(PUBLISH_FINDING_TO_SNS_PARAMS)
            .build();

    protected static final PublishFindingToSnsParams PUBLISH_FINDING_TO_SNS_PARAMS2 =
            PublishFindingToSnsParams.builder()
            .topicArn("arn:aws:sns:us-east-1:072938559174:myTopic2.fifo")
            .build();

    protected static final ActionParams ACTION_PARAMS3 = ActionParams.builder()
            .publishFindingToSnsParams(PUBLISH_FINDING_TO_SNS_PARAMS2)
            .build();

    protected static final software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams SDK_PUBLISH_FINDING_TO_SNS_PARAMS =
            software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams.builder()
                    .topicArn("arn:aws:sns:us-east-1:072938559174:myTopic.fifo")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS2 =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .publishFindingToSnsParams(SDK_PUBLISH_FINDING_TO_SNS_PARAMS)
                    .build();

    protected static final software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams SDK_PUBLISH_FINDING_TO_SNS_PARAMS2 =
            software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams.builder()
                    .topicArn("arn:aws:sns:us-east-1:072938559174:myTopic2.fifo")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS3 =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .publishFindingToSnsParams(SDK_PUBLISH_FINDING_TO_SNS_PARAMS2)
                    .build();

    protected static final Set<Tag> MODEL_TAGS = ImmutableSet.of(
            Tag.builder()
                    .key("resourceTagKey")
                    .value("resourceTagValue")
                    .build());
    protected static final Map<String, String> DESIRED_TAGS = ImmutableMap.of(
            "resourceTagKey", "resourceTagValue",
            "aws:cloudformation:stack-name", "UnitTestStack");
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_MODEL_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("resourceTagKey")
                    .value("resourceTagValue")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_SYSTEM_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("aws:cloudformation:stack-name")
                    .value("UnitTestStack")
                    .build();
}