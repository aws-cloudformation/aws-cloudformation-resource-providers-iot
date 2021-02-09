package com.amazonaws.iot.custommetric;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

public class TestConstants {

    protected static final String CUSTOM_METRIC_NAME = "TestCustomMetricName";
    protected static final String CUSTOM_METRIC_NAME2 = "TestCustomMetricName2";

    protected static final String CUSTOM_METRIC_ARN = "arn:aws:iot:us-east-1:123456789012:custommetric" +
            "/TestCustomMetricName";

    protected static final String DISPLAY_NAME = "TestDisplayName";
    protected static final String DISPLAY_NAME2 = "TestDisplayName2";
    protected static final String METRIC_TYPE = "number";

    protected static final String CLIENT_REQUEST_TOKEN = "b99b5ee6";

    protected static final Set<Tag> MODEL_TAGS = ImmutableSet.of(
            Tag.builder()
                    .key("resourceTagKey")
                    .value("resourceTagValue")
                    .build());
    protected static final Map<String, String> DESIRED_TAGS = ImmutableMap.of(
            "resourceTagKey", "resourceTagValue");
    static final Map<String, String> SYSTEM_TAG_MAP = ImmutableMap.of(
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
