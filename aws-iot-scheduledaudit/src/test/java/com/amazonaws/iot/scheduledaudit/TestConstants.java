package com.amazonaws.iot.scheduledaudit;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestConstants {

    protected static final String SCHEDULED_AUDIT_NAME = "TestScheduledAuditName";

    protected static final List<String> TARGET_CHECK_NAMES = new ArrayList<String>(
            Arrays.asList("CA_CERTIFICATE_EXPIRING_CHECK", "REVOKED_CA_CERT_CHECK")
    );
    protected static final String SCHEDULED_AUDIT_ARN = "arn:aws:iot:us-east-1:123456789012:scheduledaudit" +
            "/TestScheduledAuditName";

    protected static final String FREQUENCY = "DAILY";
    protected static final String DAY_OF_WEEK = "TUE";

    protected static final String SCHEDULED_AUDIT_NAME_2 = "TestScheduledAuditName2";

    protected static final String SCHEDULED_AUDIT_ARN_2 = "arn:aws:iot:us-east-1:123456789012:scheduledaudit" +
            "/TestScheduledAuditName2";

    protected static final String FREQUENCY_2 = "DAILY";
    protected static final String DAY_OF_WEEK_2 = "WED";

    protected static final String CLIENT_REQUEST_TOKEN = "b99b5ee6";

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
