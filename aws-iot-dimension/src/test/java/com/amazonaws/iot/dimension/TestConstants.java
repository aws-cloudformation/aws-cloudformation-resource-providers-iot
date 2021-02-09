package com.amazonaws.iot.dimension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class TestConstants {

    protected static final String DIMENSION_NAME = "TestDimensionName";
    protected static final String DIMENSION_TYPE = "TOPIC_FILTER";
    protected static final List<String> DIMENSION_VALUE_IOT = Arrays.asList("value1", "value2");
    protected static final Set<String> DIMENSION_VALUE_CFN = ImmutableSet.of("value1", "value2");
    protected static final String CLIENT_REQUEST_TOKEN = "b99b5ee6";
    protected static final String DIMENSION_ARN = "arn:aws:iot:us-east-1:123456789012:dimension/TestDimensionName";

    protected static final Set<Tag> MODEL_TAGS = ImmutableSet.of(
            Tag.builder()
                    .key("resourceTagKey1")
                    .value("resourceTagValue1")
                    .build());
    protected static final Map<String, String> DESIRED_TAGS = ImmutableMap.of(
            "resourceTagKey1", "resourceTagValue1",
            "resourceTagKey2", "resourceTagValue2");
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_MODEL_TAG_1 =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("resourceTagKey1")
                    .value("resourceTagValue1")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_MODEL_TAG_2 =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("resourceTagKey2")
                    .value("resourceTagValue2")
                    .build();
    protected static final Map<String, String> SYSTEM_TAG_MAP = ImmutableMap.of(
            "aws:cloudformation:stack-name", "UnitTestStack");
    protected static final software.amazon.awssdk.services.iot.model.Tag SDK_SYSTEM_TAG =
            software.amazon.awssdk.services.iot.model.Tag.builder()
                    .key("aws:cloudformation:stack-name")
                    .value("UnitTestStack")
                    .build();

}
