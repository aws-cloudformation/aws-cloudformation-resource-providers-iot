package com.amazonaws.iot.securityprofile;

import static com.amazonaws.iot.securityprofile.TestConstants.BEHAVIOR_NAME;
import static com.amazonaws.iot.securityprofile.Translator.translateBehaviorListFromIotToCfn;
import static com.amazonaws.iot.securityprofile.Translator.translateBehaviorSetFromCfnToIot;
import static com.amazonaws.iot.securityprofile.Translator.translateMetricValueFromCfnToIot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

public class TranslatorTest {

    @Test
    void translateBehaviors_SeveralLegalBehaviors_VerifyRoundTripTranslation() {

        Behavior scalarMetricBehaviorCfn = buildScalarMetricBehaviorCfn();
        software.amazon.awssdk.services.iot.model.Behavior scalarMetricBehaviorIot = buildScalarMetricBehaviorIot();

        Behavior sourceIpBehaviorCfn = buildSourceIpBehaviorCfn();
        software.amazon.awssdk.services.iot.model.Behavior sourceIpBehaviorIot = buildSourceIpBehaviorIot();

        Behavior listeningPortsBehaviorCfn = buildListeningPortsBehaviorCfn();
        software.amazon.awssdk.services.iot.model.Behavior listeningPortsBehaviorIot =
                buildListeningPortsBehaviorIot();

        Behavior statisticalThresholdBehaviorCfn = buildStatisticalThresholdBehaviorCfn();
        software.amazon.awssdk.services.iot.model.Behavior statisticalThresholdBehaviorIot =
                buildStatisticalThresholdBehaviorIot();

        Set<Behavior> cfnBehaviors = ImmutableSet.of(
                sourceIpBehaviorCfn, listeningPortsBehaviorCfn, statisticalThresholdBehaviorCfn,
                scalarMetricBehaviorCfn);
        Set<software.amazon.awssdk.services.iot.model.Behavior> iotBehaviors = ImmutableSet.of(
                sourceIpBehaviorIot, listeningPortsBehaviorIot, statisticalThresholdBehaviorIot,
                scalarMetricBehaviorIot);

        Set<software.amazon.awssdk.services.iot.model.Behavior> actualIotBehaviors =
                translateBehaviorSetFromCfnToIot(cfnBehaviors);
        assertThat(actualIotBehaviors).isEqualTo(iotBehaviors);

        Set<Behavior> actualCfnBehaviors = translateBehaviorListFromIotToCfn(new ArrayList<>(iotBehaviors));
        assertThat(actualCfnBehaviors).isEqualTo(cfnBehaviors);
    }

    private Behavior buildSourceIpBehaviorCfn() {
        BehaviorCriteria behaviorCriteria = BehaviorCriteria.builder()
                .comparisonOperator("in-cidr-set")
                .value(MetricValue.builder()
                        .cidrs(ImmutableSet.of(
                                "192.168.100.14/24",
                                "192.168.101.14/24",
                                "192.168.102.14/24"))
                        .build())
                .consecutiveDatapointsToAlarm(5)
                .build();
        return Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:source-ip-address")
                .criteria(behaviorCriteria)
                .build();
    }

    private software.amazon.awssdk.services.iot.model.Behavior buildSourceIpBehaviorIot() {
        software.amazon.awssdk.services.iot.model.BehaviorCriteria behaviorCriteria =
                software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                        .comparisonOperator("in-cidr-set")
                        .value(software.amazon.awssdk.services.iot.model.MetricValue.builder()
                                .cidrs(ImmutableSet.of(
                                        "192.168.100.14/24",
                                        "192.168.101.14/24",
                                        "192.168.102.14/24"))
                                .build())
                        .consecutiveDatapointsToAlarm(5)
                        .build();
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:source-ip-address")
                .criteria(behaviorCriteria)
                .build();
    }

    private Behavior buildListeningPortsBehaviorCfn() {
        BehaviorCriteria behaviorCriteria = BehaviorCriteria.builder()
                .comparisonOperator("in-port-set")
                .value(MetricValue.builder()
                        .ports(ImmutableSet.of(40, 443))
                        .build())
                .consecutiveDatapointsToClear(5)
                .build();
        return Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:listening-tcp-ports")
                .criteria(behaviorCriteria)
                .build();
    }

    private software.amazon.awssdk.services.iot.model.Behavior buildListeningPortsBehaviorIot() {
        software.amazon.awssdk.services.iot.model.BehaviorCriteria behaviorCriteria =
                software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                        .comparisonOperator("in-port-set")
                        .value(software.amazon.awssdk.services.iot.model.MetricValue.builder()
                                .ports(ImmutableSet.of(40, 443))
                                .build())
                        .consecutiveDatapointsToClear(5)
                        .build();
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:listening-tcp-ports")
                .criteria(behaviorCriteria)
                .build();
    }

    private Behavior buildStatisticalThresholdBehaviorCfn() {
        BehaviorCriteria behaviorCriteria = BehaviorCriteria.builder()
                .comparisonOperator("greater-than")
                .durationSeconds(300)
                .statisticalThreshold(StatisticalThreshold.builder().statistic("p90").build())
                .build();
        return Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-authorization-failures")
                .criteria(behaviorCriteria)
                .build();
    }

    private software.amazon.awssdk.services.iot.model.Behavior buildStatisticalThresholdBehaviorIot() {
        software.amazon.awssdk.services.iot.model.BehaviorCriteria behaviorCriteria =
                software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                        .comparisonOperator("greater-than")
                        .durationSeconds(300)
                        .statisticalThreshold(software.amazon.awssdk.services.iot.model.StatisticalThreshold
                                .builder().statistic("p90").build())
                        .build();
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-authorization-failures")
                .criteria(behaviorCriteria)
                .build();
    }

    private Behavior buildScalarMetricBehaviorCfn() {
        BehaviorCriteria behaviorCriteria = BehaviorCriteria.builder()
                .comparisonOperator("less-than")
                .durationSeconds(600)
                .value(MetricValue
                        .builder().count("999999999999").build())
                .build();
        return Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-messages-sent")
                .criteria(behaviorCriteria)
                .build();
    }

    private software.amazon.awssdk.services.iot.model.Behavior buildScalarMetricBehaviorIot() {
        software.amazon.awssdk.services.iot.model.BehaviorCriteria behaviorCriteria =
                software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                        .comparisonOperator("less-than")
                        .durationSeconds(600)
                        .value(software.amazon.awssdk.services.iot.model.MetricValue
                                .builder().count(999999999999L).build())
                        .build();
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-messages-sent")
                .criteria(behaviorCriteria)
                .build();
    }

    @Test
    void translateMetricValueFromCfnToIot_ScalarValue_NotANumber() {
        MetricValue cfnMetricValue = MetricValue.builder()
                .count("123IllegalInput")
                .build();
        assertThatThrownBy(() -> translateMetricValueFromCfnToIot(cfnMetricValue))
                .isInstanceOf(CfnInvalidRequestException.class);
    }
}
