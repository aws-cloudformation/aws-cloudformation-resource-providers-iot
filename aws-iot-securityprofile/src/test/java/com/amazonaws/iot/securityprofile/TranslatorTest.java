package com.amazonaws.iot.securityprofile;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.amazonaws.iot.securityprofile.TestConstants.BEHAVIOR_NAME;
import static com.amazonaws.iot.securityprofile.Translator.translateBehaviorListFromIotToCfn;
import static com.amazonaws.iot.securityprofile.Translator.translateBehaviorSetFromCfnToIot;
import static com.amazonaws.iot.securityprofile.Translator.translateMetricValueFromCfnToIot;
import static com.amazonaws.iot.securityprofile.Translator.translateMetricValueFromIotToCfn;
import static org.mockito.Mockito.mock;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void translateIotExceptionToCfn_AccessDeniedErrorCode() {

        HandlerErrorCode result =
                Translator.translateExceptionToErrorCode(IotException.builder().statusCode(403)
                        .message("User not authorised to perform on resource with an explicit deny " +
                                "(Service: Iot, Status Code: 403, Request ID: dummy, " +
                                "Extended Request ID: null), stack trace")
                        .build(), mock(Logger.class));
        assertThat(result).isEqualByComparingTo(HandlerErrorCode.AccessDenied);
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
                .mlDetectionConfig(MachineLearningDetectionConfig
                        .builder().confidenceLevel("HIGH").build())
                .build();
        return Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-authorization-failures")
                .criteria(behaviorCriteria)
                .suppressAlerts(true)
                .build();
    }

    private software.amazon.awssdk.services.iot.model.Behavior buildStatisticalThresholdBehaviorIot() {
        software.amazon.awssdk.services.iot.model.BehaviorCriteria behaviorCriteria =
                software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                        .comparisonOperator("greater-than")
                        .durationSeconds(300)
                        .statisticalThreshold(software.amazon.awssdk.services.iot.model.StatisticalThreshold
                                .builder().statistic("p90").build())
                        .mlDetectionConfig(software.amazon.awssdk.services.iot.model.MachineLearningDetectionConfig
                                .builder().confidenceLevel("HIGH").build())
                        .build();
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-authorization-failures")
                .criteria(behaviorCriteria)
                .suppressAlerts(true)
                .build();
    }

    private Behavior buildScalarMetricBehaviorCfn() {
        BehaviorCriteria behaviorCriteria = BehaviorCriteria.builder()
                .comparisonOperator("less-than")
                .durationSeconds(600)
                .value(MetricValue
                        .builder().count("999999999999").build())
                .mlDetectionConfig(MachineLearningDetectionConfig
                        .builder().confidenceLevel("MEDIUM").build())
                .build();
        return Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-messages-sent")
                .criteria(behaviorCriteria)
                .suppressAlerts(false)
                .build();
    }

    private software.amazon.awssdk.services.iot.model.Behavior buildScalarMetricBehaviorIot() {
        software.amazon.awssdk.services.iot.model.BehaviorCriteria behaviorCriteria =
                software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                        .comparisonOperator("less-than")
                        .durationSeconds(600)
                        .value(software.amazon.awssdk.services.iot.model.MetricValue
                                .builder().count(999999999999L).build())
                        .mlDetectionConfig(software.amazon.awssdk.services.iot.model.MachineLearningDetectionConfig
                                .builder().confidenceLevel("MEDIUM").build())
                        .build();
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(BEHAVIOR_NAME)
                .metric("aws:num-messages-sent")
                .criteria(behaviorCriteria)
                .suppressAlerts(false)
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

    @Test
    void translateMetricValueFromCfnToIot_VerifyTranslation() {
        Set<Double> setOfDoubles = ImmutableSet.of(123d, 456d);
        Double numberDouble = 123d;
        Set<String> setOfStrings = ImmutableSet.of("123", "456");
        String stringCount = "123";
        MetricValue cfnMetricValue = MetricValue.builder()
                .count(stringCount)
                .cidrs(setOfStrings)
                .number(123d)
                .numbers(setOfDoubles)
                .strings(setOfStrings)
                .build();
        software.amazon.awssdk.services.iot.model.MetricValue metricValue =
                translateMetricValueFromCfnToIot(cfnMetricValue);
        assertTrue(metricValue.count().equals(Long.parseLong(stringCount)));
        assertTrue(metricValue.number().equals(numberDouble));
        assertTrue(metricValue.cidrs().size() == setOfStrings.size() && metricValue.cidrs().containsAll(setOfStrings));
        assertTrue(metricValue.numbers().size() == setOfDoubles.size() && metricValue.numbers().containsAll(setOfDoubles));
        assertTrue(metricValue.strings().size() == setOfStrings.size() && metricValue.strings().containsAll(setOfStrings));
    }

    @Test
    void translateMetricValueFromIotToCfn_VerifyTranslation() {
        List<String> listOfStrings = Arrays.asList("123", "456");
        List<Double> listOfDoubles = Arrays.asList(123d, 456d);
        Double numberDouble = 123d;
        Long numberLong = 123l;
        software.amazon.awssdk.services.iot.model.MetricValue iotMetricValue = software.amazon.awssdk.services.iot.model.MetricValue.builder()
                .count(numberLong)
                .number(numberDouble)
                .numbers(listOfDoubles)
                .cidrs(listOfStrings)
                .strings(listOfStrings)
                .build();
        MetricValue metricValue = translateMetricValueFromIotToCfn(iotMetricValue);
        assertTrue(metricValue.getCount().equals(numberLong.toString()));
        assertTrue(metricValue.getNumber().equals(numberDouble));
        assertTrue(metricValue.getNumbers().size() == listOfDoubles.size() && metricValue.getNumbers().containsAll(listOfDoubles));
        assertTrue(metricValue.getCidrs().size() == listOfStrings.size() && metricValue.getCidrs().containsAll(listOfStrings));
        assertTrue(metricValue.getStrings().size() == listOfStrings.size() && metricValue.getStrings().containsAll(listOfStrings));
    }
}
