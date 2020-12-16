package com.amazonaws.iot.securityprofile;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import software.amazon.awssdk.services.iot.model.InternalFailureException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class Translator {

    static Set<software.amazon.awssdk.services.iot.model.Behavior> translateBehaviorSetFromCfnToIot(
            Set<Behavior> cfnBehaviors) {
        if (cfnBehaviors == null) {
            return null;
        }
        return cfnBehaviors.stream()
                .map(Translator::translateBehaviorFromCfnToIot)
                .collect(Collectors.toSet());
    }

    static Set<Behavior> translateBehaviorListFromIotToCfn(
            @NonNull List<software.amazon.awssdk.services.iot.model.Behavior> iotBehaviors) {
        return iotBehaviors.stream()
                .map(Translator::translateBehaviorFromIotToCfn)
                .collect(Collectors.toSet());
    }

    private static software.amazon.awssdk.services.iot.model.Behavior translateBehaviorFromCfnToIot(
            Behavior cfnBehavior) {
        if (cfnBehavior == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.Behavior.builder()
                .name(cfnBehavior.getName())
                .metric(cfnBehavior.getMetric())
                .metricDimension(translateMetricDimensionFromCfnToIot(cfnBehavior.getMetricDimension()))
                .criteria(translateBehaviorCriteriaFromCfnToIot(cfnBehavior.getCriteria()))
                .build();
    }

    private static Behavior translateBehaviorFromIotToCfn(
            @NonNull software.amazon.awssdk.services.iot.model.Behavior iotBehavior) {
        return Behavior.builder()
                .name(iotBehavior.name())
                .metric(iotBehavior.metric())
                .metricDimension(translateMetricDimensionFromIotToCfn(iotBehavior.metricDimension()))
                .criteria(translateBehaviorCriteriaFromIotToCfn(iotBehavior.criteria()))
                .build();
    }

    private static software.amazon.awssdk.services.iot.model.BehaviorCriteria translateBehaviorCriteriaFromCfnToIot(
            BehaviorCriteria cfnCriteria) {
        if (cfnCriteria == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.BehaviorCriteria.builder()
                .comparisonOperator(cfnCriteria.getComparisonOperator())
                .value(translateMetricValueFromCfnToIot(cfnCriteria.getValue()))
                .durationSeconds(cfnCriteria.getDurationSeconds())
                .consecutiveDatapointsToAlarm(cfnCriteria.getConsecutiveDatapointsToAlarm())
                .consecutiveDatapointsToClear(cfnCriteria.getConsecutiveDatapointsToClear())
                .statisticalThreshold(translateStatisticalThresholdFromCfnToIot(cfnCriteria.getStatisticalThreshold()))
                .build();
    }

    private static BehaviorCriteria translateBehaviorCriteriaFromIotToCfn(
            software.amazon.awssdk.services.iot.model.BehaviorCriteria iotCriteria) {
        if (iotCriteria == null) {
            return null;
        }
        return BehaviorCriteria.builder()
                .comparisonOperator(iotCriteria.comparisonOperatorAsString())
                .value(translateMetricValueFromIotToCfn(iotCriteria.value()))
                .durationSeconds(iotCriteria.durationSeconds())
                .consecutiveDatapointsToAlarm(iotCriteria.consecutiveDatapointsToAlarm())
                .consecutiveDatapointsToClear(iotCriteria.consecutiveDatapointsToClear())
                .statisticalThreshold(translateStatisticalThresholdFromIotToCfn(iotCriteria.statisticalThreshold()))
                .build();
    }

    static software.amazon.awssdk.services.iot.model.MetricValue translateMetricValueFromCfnToIot(
            MetricValue cfnMetricValue) {
        if (cfnMetricValue == null) {
            return null;
        }
        // The MetricValue is a String in the CFN model because the json-schema doesn't support Long numbers.
        // This is a known issue and some existing services are already using Strings as the workaround.
        // For example, AWS::ElasticLoadBalancing::LoadBalancer HealthCheck
        // https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-ec2-elb-health-check.html
        String countStringFromCfn = cfnMetricValue.getCount();
        Long countLong;
        if (countStringFromCfn == null) {
            countLong = null;
        } else {
            try {
                countLong = Long.parseLong(countStringFromCfn);
            } catch (NumberFormatException e) {
                throw new CfnInvalidRequestException("Invalid value for Behavior::MetricValue::Count: " +
                                                     countStringFromCfn);
            }
        }

        return software.amazon.awssdk.services.iot.model.MetricValue.builder()
                .count(countLong)
                .cidrs(cfnMetricValue.getCidrs())
                .ports(cfnMetricValue.getPorts())
                .build();
    }

    static MetricValue translateMetricValueFromIotToCfn(
            software.amazon.awssdk.services.iot.model.MetricValue iotMetricValue) {
        if (iotMetricValue == null) {
            return null;
        }

        MetricValue.MetricValueBuilder metricValueBuilder = MetricValue.builder();
        if (iotMetricValue.count() != null) {
            metricValueBuilder.count(iotMetricValue.count().toString());
        }
        // For lists, we're using the .has* methods to differentiate between null and empty lists
        // from DescribeSecurityProfileResponse.
        // SDK converts nulls from Describe API to empty DefaultSdkAutoConstructLists,
        // so if we simply translate without the .has* check, nulls will turn into empty lists.
        if (iotMetricValue.hasCidrs()) {
            metricValueBuilder.cidrs(new HashSet<>(iotMetricValue.cidrs()));
        }
        if (iotMetricValue.hasPorts()) {
            metricValueBuilder.ports(new HashSet<>(iotMetricValue.ports()));
        }

        return metricValueBuilder.build();
    }

    private static software.amazon.awssdk.services.iot.model.MetricDimension translateMetricDimensionFromCfnToIot(
            MetricDimension cfnMetricDimension) {
        if (cfnMetricDimension == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.MetricDimension.builder()
                .dimensionName(cfnMetricDimension.getDimensionName())
                .operator(cfnMetricDimension.getOperator())
                .build();
    }

    private static MetricDimension translateMetricDimensionFromIotToCfn(
            software.amazon.awssdk.services.iot.model.MetricDimension iotMetricDimension) {
        if (iotMetricDimension == null) {
            return null;
        }
        return MetricDimension.builder()
                .dimensionName(iotMetricDimension.dimensionName())
                .operator(iotMetricDimension.operatorAsString())
                .build();
    }

    private static software.amazon.awssdk.services.iot.model.StatisticalThreshold translateStatisticalThresholdFromCfnToIot(
            StatisticalThreshold cfnThreshold) {
        if (cfnThreshold == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.StatisticalThreshold.builder()
                .statistic(cfnThreshold.getStatistic())
                .build();
    }

    private static StatisticalThreshold translateStatisticalThresholdFromIotToCfn(
            software.amazon.awssdk.services.iot.model.StatisticalThreshold iotThreshold) {
        if (iotThreshold == null) {
            return null;
        }
        return StatisticalThreshold.builder()
                .statistic(iotThreshold.statistic())
                .build();
    }

    static Map<String, software.amazon.awssdk.services.iot.model.AlertTarget> translateAlertTargetMapFromCfnToIot(
            Map<String, AlertTarget> cfnAlertTargetMap) {
        if (cfnAlertTargetMap == null) {
            return null;
        }
        return cfnAlertTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> translateAlertTargetFromCfnToIot(entry.getValue())));
    }

    static Map<String, AlertTarget> translateAlertTargetMapFromIotToCfn(
            @NonNull Map<String, software.amazon.awssdk.services.iot.model.AlertTarget> iotAlertTargetMap) {

        return iotAlertTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> translateAlertTargetFromIotToCfn(entry.getValue())));
    }

    private static software.amazon.awssdk.services.iot.model.AlertTarget translateAlertTargetFromCfnToIot(
            AlertTarget cfnAlertTarget) {
        if (cfnAlertTarget == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.AlertTarget.builder()
                .alertTargetArn(cfnAlertTarget.getAlertTargetArn())
                .roleArn(cfnAlertTarget.getRoleArn())
                .build();
    }

    private static AlertTarget translateAlertTargetFromIotToCfn(
            @NonNull software.amazon.awssdk.services.iot.model.AlertTarget iotAlertTarget) {
        return AlertTarget.builder()
                .alertTargetArn(iotAlertTarget.alertTargetArn())
                .roleArn(iotAlertTarget.roleArn())
                .build();
    }

    static Set<software.amazon.awssdk.services.iot.model.MetricToRetain> translateMetricToRetainSetFromCfnToIot(
            Set<MetricToRetain> cfnMetricToRetainSet) {
        if (cfnMetricToRetainSet == null) {
            return null;
        }
        return cfnMetricToRetainSet.stream()
                .map(Translator::translateMetricToRetainFromCfnToIot)
                .collect(Collectors.toSet());
    }

    static Set<MetricToRetain> translateMetricToRetainListFromIotToCfn(
            @NonNull List<software.amazon.awssdk.services.iot.model.MetricToRetain> iotMetricToRetainList) {
        return iotMetricToRetainList.stream()
                .map(Translator::translateMetricToRetainFromIotToCfn)
                .collect(Collectors.toSet());
    }

    private static software.amazon.awssdk.services.iot.model.MetricToRetain translateMetricToRetainFromCfnToIot(
            MetricToRetain cfnMetricToRetain) {
        if (cfnMetricToRetain == null) {
            return null;
        }
        return software.amazon.awssdk.services.iot.model.MetricToRetain.builder()
                .metric(cfnMetricToRetain.getMetric())
                .metricDimension(translateMetricDimensionFromCfnToIot(cfnMetricToRetain.getMetricDimension()))
                .build();
    }

    private static MetricToRetain translateMetricToRetainFromIotToCfn(
            @NonNull software.amazon.awssdk.services.iot.model.MetricToRetain iotMetricToRetain) {
        return MetricToRetain.builder()
                .metric(iotMetricToRetain.metric())
                .metricDimension(translateMetricDimensionFromIotToCfn(iotMetricToRetain.metricDimension()))
                .build();
    }

    static Set<software.amazon.awssdk.services.iot.model.Tag> translateTagsFromCfnToIot(Map<String, String> tags) {
        if (tags == null) {
            return null;
        }
        return tags.keySet().stream()
                .map(key -> Tag.builder()
                        .key(key)
                        .value(tags.get(key))
                        .build())
                .collect(Collectors.toSet());
    }

    static Set<com.amazonaws.iot.securityprofile.Tag> translateTagsFromIotToCfn(
            Set<Tag> tags) {
        if (tags == null) {
            return null;
        }
        return tags.stream()
                .map(tag -> com.amazonaws.iot.securityprofile.Tag.builder()
                        .key(tag.key())
                        .value(tag.value())
                        .build())
                .collect(Collectors.toSet());
    }

    static ProgressEvent<ResourceModel, CallbackContext> translateExceptionToProgressEvent(
            ResourceModel model, Exception e, Logger logger) {

        HandlerErrorCode errorCode = translateExceptionToProgressEvent(e, logger);
        ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .errorCode(errorCode)
                        .build();
        if (errorCode != HandlerErrorCode.InternalFailure) {
            progressEvent.setMessage(e.getMessage());
        }
        return progressEvent;
    }

    private static HandlerErrorCode translateExceptionToProgressEvent(Exception e, Logger logger) {

        logger.log(String.format("Translating exception \"%s\", stack trace: %s",
                e.getMessage(), ExceptionUtils.getStackTrace(e)));

        // We're handling all the exceptions documented in API docs
        // https://docs.aws.amazon.com/iot/latest/apireference/API_CreateSecurityProfile.html
        // (+similar pages for other APIs)
        // For Throttling and InternalFailure, we want CFN to retry, and it will do so based on the error code.
        // Reference with Retriable/Terminal in comments for each: https://tinyurl.com/y378qdno
        if (e instanceof ResourceAlreadyExistsException) {
            // Note regarding idempotency:
            // CreateSecurityProfile API allows tags. CFN attaches its own stack level tags with the request. If a
            // SecurityProfile is created out of band and then the same request is sent via CFN, the API will throw
            // AlreadyExists because the CFN request will contain the stack level tags.
            // This behavior satisfies the CreateHandler contract.
            return HandlerErrorCode.AlreadyExists;
        } else if (e instanceof InvalidRequestException) {
            return HandlerErrorCode.InvalidRequest;
        } else if (e instanceof LimitExceededException) {
            return HandlerErrorCode.ServiceLimitExceeded;
        } else if (e instanceof UnauthorizedException) {
            return HandlerErrorCode.AccessDenied;
        } else if (e instanceof InternalFailureException) {
            return HandlerErrorCode.InternalFailure;
        } else if (e instanceof ThrottlingException) {
            return HandlerErrorCode.Throttling;
        } else if (e instanceof ResourceNotFoundException) {
            return HandlerErrorCode.NotFound;
        } else {
            logger.log(String.format("Unexpected exception \"%s\", stack trace: %s",
                    e.getMessage(), ExceptionUtils.getStackTrace(e)));
            // Any other exception at this point is unexpected.
            return HandlerErrorCode.InternalFailure;
        }
    }
}
