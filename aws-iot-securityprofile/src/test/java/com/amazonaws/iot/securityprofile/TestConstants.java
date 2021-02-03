package com.amazonaws.iot.securityprofile;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import software.amazon.awssdk.services.iot.model.ComparisonOperator;

public class TestConstants {

    static final String SECURITY_PROFILE_NAME = "TestSecurityProfile";
    static final String SECURITY_PROFILE_ARN =
            "arn:aws:iot:us-east-1:123456789012:dimension/TestSecurityProfile";
    static final String SECURITY_PROFILE_DESCRIPTION = "TestDescription";
    static final String CLIENT_REQUEST_TOKEN = "TestToken";
    static final String LOGICAL_IDENTIFIER = "LogicalIdentifier";
    static final String BEHAVIOR_NAME = "testBehavior";
    static final MetricDimension DIMENSION_CFN = MetricDimension.builder()
            .dimensionName("TestDimension")
            .operator("NOT_IN")
            .build();
    static final MetricToRetain METRIC_TO_RETAIN_CFN = MetricToRetain.builder()
            .metric("aws:num-messages-sent")
            .metricDimension(DIMENSION_CFN)
            .build();
    static final software.amazon.awssdk.services.iot.model.MetricDimension DIMENSION_IOT =
            software.amazon.awssdk.services.iot.model.MetricDimension.builder()
                    .dimensionName("TestDimension")
                    .operator("NOT_IN")
                    .build();
    static final software.amazon.awssdk.services.iot.model.MetricToRetain METRIC_TO_RETAIN_IOT =
            software.amazon.awssdk.services.iot.model.MetricToRetain.builder()
                    .metric("aws:num-messages-sent")
                    .metricDimension(DIMENSION_IOT)
                    .build();
    static final Set<MetricToRetain> ADDITIONAL_METRICS_CFN = ImmutableSet.of(METRIC_TO_RETAIN_CFN);
    static final List<software.amazon.awssdk.services.iot.model.MetricToRetain> ADDITIONAL_METRICS_IOT =
            ImmutableList.of(METRIC_TO_RETAIN_IOT);
    static final String TARGET_ARN_1 = "arn:aws:iot:us-west-2:123456789012:all/unregistered-things";
    static final String TARGET_ARN_2 = "arn:aws:iot:us-west-2:123456789012:all/registered-things";
    static final Set<String> TARGET_ARN_1_SET = ImmutableSet.of(TARGET_ARN_1);
    static final Set<String> TARGET_ARN_2_SET = ImmutableSet.of(TARGET_ARN_2);
    static final Set<String> TARGET_ARNS = ImmutableSet.of(TARGET_ARN_1, TARGET_ARN_2);
    static final String TAG_1_KEY = "TagKey1";
    static final List<String> TAG_1_KEY_LIST = ImmutableList.of(TAG_1_KEY);
    static final Set<Tag> TAG_1_CFN_SET = ImmutableSet.of(
            Tag.builder()
                    .key(TAG_1_KEY)
                    .value("TagValue1")
                    .build());
    static final Set<Tag> TAG_1_AND_SYSTEM_TAG_CFN_SET = ImmutableSet.of(
            Tag.builder()
                    .key(TAG_1_KEY)
                    .value("TagValue1")
                    .build(),
            Tag.builder()
                    .key("aws:cloudformation:stack-name")
                    .value("UnitTestStack")
                    .build());
    static final Map<String, String> TAG_1_STRINGMAP = ImmutableMap.of(
            TAG_1_KEY, "TagValue1");
    static final Map<String, String> TAG_2_STRINGMAP = ImmutableMap.of(
            "TagKey2", "TagValue2");
    static final Map<String, String> SYSTEM_TAG_MAP = ImmutableMap.of(
            "aws:cloudformation:stack-name", "UnitTestStack");
    static final software.amazon.awssdk.services.iot.model.Tag TAG_1_IOT =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key(TAG_1_KEY)
                    .value("TagValue1")
                    .build();
    static final software.amazon.awssdk.services.iot.model.Tag TAG_2_IOT =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("TagKey2")
                    .value("TagValue2")
                    .build();
    static final software.amazon.awssdk.services.iot.model.Tag SYSTEM_TAG_IOT =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("aws:cloudformation:stack-name")
                    .value("UnitTestStack")
                    .build();
    static final Set<software.amazon.awssdk.services.iot.model.Tag> TAG_1_IOT_SET =
            ImmutableSet.of(TAG_1_IOT);
    static final List<software.amazon.awssdk.services.iot.model.Tag> TAG_2_IOT_LIST =
            ImmutableList.of(TAG_2_IOT);
    static final Set<software.amazon.awssdk.services.iot.model.Tag> TAGS_IOT =
            ImmutableSet.of(TAG_1_IOT, TAG_2_IOT);
    static final software.amazon.awssdk.services.iot.model.SecurityProfileTarget SECURITY_PROFILE_TARGET_1 =
            software.amazon.awssdk.services.iot.model.SecurityProfileTarget.builder()
                    .arn(TARGET_ARN_1)
                    .build();
    static final software.amazon.awssdk.services.iot.model.SecurityProfileTarget SECURITY_PROFILE_TARGET_2 =
            software.amazon.awssdk.services.iot.model.SecurityProfileTarget.builder()
                    .arn(TARGET_ARN_2)
                    .build();
    static final software.amazon.awssdk.services.iot.model.BehaviorCriteria CRITERIA_1_IOT =
            software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                    .comparisonOperator(ComparisonOperator.GREATER_THAN)
                    .build();
    static final software.amazon.awssdk.services.iot.model.Behavior BEHAVIOR_1_IOT =
            software.amazon.awssdk.services.iot.model.Behavior.builder()
                    .name(BEHAVIOR_NAME)
                    .metric("aws:message-byte-size")
                    .metricDimension(DIMENSION_IOT)
                    .criteria(CRITERIA_1_IOT)
                    .build();
    static final List<software.amazon.awssdk.services.iot.model.Behavior> BEHAVIOR_1_IOT_LIST =
            ImmutableList.of(BEHAVIOR_1_IOT);
    static final BehaviorCriteria CRITERIA_1_CFN = BehaviorCriteria.builder()
            .comparisonOperator("greater-than")
            .build();
    static final Behavior BEHAVIOR_1_CFN = Behavior.builder()
            .name(BEHAVIOR_NAME)
            .metric("aws:message-byte-size")
            .metricDimension(DIMENSION_CFN)
            .criteria(CRITERIA_1_CFN)
            .build();
    static final Set<Behavior> BEHAVIOR_1_CFN_SET = ImmutableSet.of(BEHAVIOR_1_CFN);
    static final software.amazon.awssdk.services.iot.model.AlertTarget ALERT_TARGET_IOT =
            software.amazon.awssdk.services.iot.model.AlertTarget.builder()
                    .alertTargetArn("testAlertTargetArn")
                    .roleArn("testRoleArn")
                    .build();
    static final AlertTarget ALERT_TARGET_CFN = AlertTarget.builder()
            .alertTargetArn("testAlertTargetArn")
            .roleArn("testRoleArn")
            .build();
    static final Map<String, software.amazon.awssdk.services.iot.model.AlertTarget> ALERT_TARGET_MAP_IOT =
            ImmutableMap.of("SNS", ALERT_TARGET_IOT);
    static final Map<String, AlertTarget> ALERT_TARGET_MAP_CFN =
            ImmutableMap.of("SNS", ALERT_TARGET_CFN);
}
