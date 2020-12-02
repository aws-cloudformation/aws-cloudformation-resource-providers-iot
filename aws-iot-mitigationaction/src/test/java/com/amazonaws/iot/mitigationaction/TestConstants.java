package com.amazonaws.iot.mitigationaction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class TestConstants {

    protected static final String MITIGATION_ACTION_NAME = "TestMitigationActionName";
    protected static final String MITIGATION_ACTION_NAME2 = "TestMitigationActionName2";
    protected static final String CLIENT_REQUEST_TOKEN = "b99b5ee6";
    protected static final String ACTION_ARN = "arn:aws:iot:us-east-1:123456789012:mitigationAction/TestMitigationActionName";
    protected static final String ACTION_ARN2 = "arn:aws:iot:us-east-1:123456789012:mitigationAction" +
            "/TestMitigationActionName2";
    protected static final String ACTION_ID = "12345-abced-6789-efgh";
    protected static final String MITIGATION_ACTION_ROLE_ARN = "arn:aws:iot:us-east-1:123456789012:Role/TestRole";
    protected static final ReplaceDefaultPolicyVersionParams REPLACE_DEFAULT_POLICY_VERSION_PARAMS =
            ReplaceDefaultPolicyVersionParams.builder()
                    .templateName("BLANK_POLICY")
                    .build();
    protected static final ActionParams ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY_VERSION_PARAMS = ActionParams.builder()
            .replaceDefaultPolicyVersionParams(REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
            .build();
    protected static final AddThingsToThingGroupParams ADD_THINGS_TO_THING_GROUP_PARAMS =
            AddThingsToThingGroupParams.builder()
                    .overrideDynamicGroups(true)
                    .thingGroupNames(ImmutableSet.of("ThingGroupNameTest1", "ThingGroupNameTest2"))
                    .build()
            ;
    protected static final ActionParams ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP_PARAMS = ActionParams.builder()
            .addThingsToThingGroupParams(ADD_THINGS_TO_THING_GROUP_PARAMS)
            .build();
    protected static final EnableIoTLoggingParams ENABLE_IOT_LOGGING_PARAMS = EnableIoTLoggingParams.builder()
            .logLevel("DEBUG")
            .roleArnForLogging("RoleArnForLoggingTest1")
            .build();
    protected static final ActionParams ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS= ActionParams.builder()
            .enableIoTLoggingParams(ENABLE_IOT_LOGGING_PARAMS)
            .build();
    protected static final UpdateCACertificateParams UPDATE_CA_CERTIFICATE_PARAMS = UpdateCACertificateParams.builder()
            .action("DEACTIVATE")
            .build();
    protected static final ActionParams ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS= ActionParams.builder()
            .updateCACertificateParams(UPDATE_CA_CERTIFICATE_PARAMS)
            .build();
    protected static final UpdateDeviceCertificateParams UPDATE_DEVICE_CERTIFICATE_PARAMS = UpdateDeviceCertificateParams.builder()
            .action("DEACTIVATE")
            .build();
    protected static final ActionParams ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS= ActionParams.builder()
            .updateDeviceCertificateParams(UPDATE_DEVICE_CERTIFICATE_PARAMS)
            .build();
    protected static final PublishFindingToSnsParams PUBLISH_FINDING_TO_SNS_PARAMS = PublishFindingToSnsParams.builder()
            .topicArn("arn:aws:sns:us-east-1:123456789012:myTopic.fifo")
            .build();

    protected static final ActionParams ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS_PARAMS = ActionParams.builder()
            .publishFindingToSnsParams(PUBLISH_FINDING_TO_SNS_PARAMS)
            .build();

    protected static final PublishFindingToSnsParams PUBLISH_FINDING_TO_SNS_PARAMS_2 =
            PublishFindingToSnsParams.builder()
                    .topicArn("arn:aws:sns:us-east-1:123456789012:myTopic2.fifo")
                    .build();

    protected static final ActionParams ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS_PARAMS_2 = ActionParams.builder()
            .publishFindingToSnsParams(PUBLISH_FINDING_TO_SNS_PARAMS_2)
            .build();


    protected static final ActionParams ACTION_PARAMS_WITH_ALL= ActionParams.builder()
            .updateDeviceCertificateParams(UPDATE_DEVICE_CERTIFICATE_PARAMS)
            .enableIoTLoggingParams(ENABLE_IOT_LOGGING_PARAMS)
            .updateCACertificateParams(UPDATE_CA_CERTIFICATE_PARAMS)
            .addThingsToThingGroupParams(ADD_THINGS_TO_THING_GROUP_PARAMS)
            .replaceDefaultPolicyVersionParams(REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
            .publishFindingToSnsParams(PUBLISH_FINDING_TO_SNS_PARAMS_2)
            .build();

    protected static final software.amazon.awssdk.services.iot.model.ReplaceDefaultPolicyVersionParams SDK_REPLACE_DEFAULT_POLICY_VERSION_PARAMS =
            software.amazon.awssdk.services.iot.model.ReplaceDefaultPolicyVersionParams.builder()
                    .templateName("BLANK_POLICY")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_REPLACE_DEFAULT_POLICY =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .replaceDefaultPolicyVersionParams(SDK_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.AddThingsToThingGroupParams SDK_ADD_THINGS_TO_THING_GROUP_PARAMS =
            software.amazon.awssdk.services.iot.model.AddThingsToThingGroupParams.builder()
                    .thingGroupNames(Arrays.asList("ThingGroupNameTest1", "ThingGroupNameTest2"))
                    .overrideDynamicGroups(true)
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_ADD_THINGS_TO_THING_GROUP =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .addThingsToThingGroupParams(SDK_ADD_THINGS_TO_THING_GROUP_PARAMS)
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.EnableIoTLoggingParams SDK_ENABLE_IOT_LOGGING_PARAMS =
            software.amazon.awssdk.services.iot.model.EnableIoTLoggingParams.builder()
                    .logLevel("DEBUG")
                    .roleArnForLogging("RoleArnForLoggingTest1")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_ENABLE_IOT_LOGGING_PARAMS =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .enableIoTLoggingParams(SDK_ENABLE_IOT_LOGGING_PARAMS)
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.UpdateCACertificateParams SDK_UPDATE_CA_CERTIFICATE_PARAMS =
            software.amazon.awssdk.services.iot.model.UpdateCACertificateParams.builder()
                    .action("DEACTIVATE")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_UPDATE_CA_CERTIFICATE_PARAMS =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .updateCACertificateParams(SDK_UPDATE_CA_CERTIFICATE_PARAMS)
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.UpdateDeviceCertificateParams SDK_UPDATE_DEVICE_CERTIFICATE_PARAMS =
            software.amazon.awssdk.services.iot.model.UpdateDeviceCertificateParams.builder()
                    .action("DEACTIVATE")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_UPDATE_DEVICE_CERTIFICATE_PARAMS =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .updateDeviceCertificateParams(SDK_UPDATE_DEVICE_CERTIFICATE_PARAMS)
                    .build();

    protected static final software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams SDK_PUBLISH_FINDING_TO_SNS_PARAMS =
            software.amazon.awssdk.services.iot.model.PublishFindingToSnsParams.builder()
                    .topicArn("arn:aws:sns:us-east-1:123456789012:myTopic2.fifo")
                    .build();
    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_PUBLISH_FINDING_TO_SNS =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .publishFindingToSnsParams(SDK_PUBLISH_FINDING_TO_SNS_PARAMS)
                    .build();

    protected static final software.amazon.awssdk.services.iot.model.MitigationActionParams SDK_ACTION_PARAMS_WITH_ALL =
            software.amazon.awssdk.services.iot.model.MitigationActionParams.builder()
                    .publishFindingToSnsParams(SDK_PUBLISH_FINDING_TO_SNS_PARAMS)
                    .updateDeviceCertificateParams(SDK_UPDATE_DEVICE_CERTIFICATE_PARAMS)
                    .updateCACertificateParams(SDK_UPDATE_CA_CERTIFICATE_PARAMS)
                    .enableIoTLoggingParams(SDK_ENABLE_IOT_LOGGING_PARAMS)
                    .addThingsToThingGroupParams(SDK_ADD_THINGS_TO_THING_GROUP_PARAMS)
                    .replaceDefaultPolicyVersionParams(SDK_REPLACE_DEFAULT_POLICY_VERSION_PARAMS)
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
