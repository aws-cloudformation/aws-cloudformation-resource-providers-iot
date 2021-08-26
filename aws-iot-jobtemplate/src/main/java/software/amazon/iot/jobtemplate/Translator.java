package software.amazon.iot.jobtemplate;

import software.amazon.awssdk.services.iot.model.AbortConfig;
import software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig;
import software.amazon.awssdk.services.iot.model.PresignedUrlConfig;
import software.amazon.awssdk.services.iot.model.RateIncreaseCriteria;
import software.amazon.awssdk.services.iot.model.TimeoutConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Translator {

    static software.amazon.awssdk.services.iot.model.AbortConfig getAbortConfig(ResourceModel model) {
        if(model == null || model.getAbortConfig() == null || model.getAbortConfig().getCriteriaList().isEmpty()) {
            return null;
        }
        List<software.amazon.iot.jobtemplate.AbortCriteria> modelCriteriaList = model.getAbortConfig().getCriteriaList();

        List<software.amazon.awssdk.services.iot.model.AbortCriteria> sdkCriteriaList = modelCriteriaList.stream().map( modelCriteria -> {
            return software.amazon.awssdk.services.iot.model.AbortCriteria.builder()
                    .action(modelCriteria.getAction())
                    .failureType(modelCriteria.getFailureType())
                    .thresholdPercentage(modelCriteria.getThresholdPercentage())
                    .minNumberOfExecutedThings(modelCriteria.getMinNumberOfExecutedThings())
                    .build();
        }).collect(Collectors.toList());
        return  AbortConfig.builder()
                .criteriaList(sdkCriteriaList)
                .build();
    }

    static software.amazon.iot.jobtemplate.AbortConfig getAbortConfig(AbortConfig config) {
        if(config == null || config.criteriaList() == null || config.criteriaList().isEmpty()) {
            return null;
        }
        List<software.amazon.awssdk.services.iot.model.AbortCriteria> sdkCriteriaList = config.criteriaList();

        List<AbortCriteria> modelCriteriaList = sdkCriteriaList.stream().map( sdkCriteria -> {
            return AbortCriteria.builder()
                    .action(sdkCriteria.actionAsString())
                    .failureType(sdkCriteria.failureTypeAsString())
                    .thresholdPercentage(sdkCriteria.thresholdPercentage())
                    .minNumberOfExecutedThings(sdkCriteria.minNumberOfExecutedThings())
                    .build();
        }).collect(Collectors.toList());

        return software.amazon.iot.jobtemplate.AbortConfig.builder()
                .criteriaList(modelCriteriaList)
                .build();
    }

    static software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig getJobExecutionsRolloutConfig(ResourceModel model) {
        if(model == null || model.getJobExecutionsRolloutConfig() == null) {
            return null;
        }
        software.amazon.iot.jobtemplate.ExponentialRolloutRate modelRolloutRate = model.getJobExecutionsRolloutConfig().getExponentialRolloutRate();

        software.amazon.awssdk.services.iot.model.RateIncreaseCriteria increaseCriteria = RateIncreaseCriteria.builder()
                .numberOfNotifiedThings(modelRolloutRate.getRateIncreaseCriteria().getNumberOfNotifiedThings())
                .numberOfSucceededThings(modelRolloutRate.getRateIncreaseCriteria().getNumberOfSucceededThings())
                .build();

        software.amazon.awssdk.services.iot.model.ExponentialRolloutRate requestRolloutRate = software.amazon.awssdk.services.iot.model.ExponentialRolloutRate.builder()
                .baseRatePerMinute(modelRolloutRate.getBaseRatePerMinute())
                .incrementFactor(modelRolloutRate.getIncrementFactor())
                .rateIncreaseCriteria(increaseCriteria)
                .build();
        return JobExecutionsRolloutConfig.builder()
                .exponentialRate(requestRolloutRate)
                .maximumPerMinute(model.getJobExecutionsRolloutConfig().getMaximumPerMinute())
                .build();
    }

    static software.amazon.iot.jobtemplate.JobExecutionsRolloutConfig getJobExecutionsRolloutConfig(JobExecutionsRolloutConfig config) {
        if(config == null || config.exponentialRate() == null || config.exponentialRate().rateIncreaseCriteria() == null) {
            return null;
        }
        software.amazon.awssdk.services.iot.model.ExponentialRolloutRate modelExponentialRolloutRate = config.exponentialRate();

        software.amazon.iot.jobtemplate.RateIncreaseCriteria rateIncreaseCriteria = software.amazon.iot.jobtemplate.RateIncreaseCriteria.builder()
                .numberOfNotifiedThings(modelExponentialRolloutRate.rateIncreaseCriteria().numberOfNotifiedThings())
                .numberOfSucceededThings(modelExponentialRolloutRate.rateIncreaseCriteria().numberOfSucceededThings())
                .build();

        ExponentialRolloutRate exponentialRolloutRate = ExponentialRolloutRate.builder()
                .baseRatePerMinute(modelExponentialRolloutRate.baseRatePerMinute())
                .incrementFactor(modelExponentialRolloutRate.incrementFactor())
                .rateIncreaseCriteria(rateIncreaseCriteria)
                .build();
        return software.amazon.iot.jobtemplate.JobExecutionsRolloutConfig.builder()
                .exponentialRolloutRate(exponentialRolloutRate)
                .maximumPerMinute(config.maximumPerMinute())
                .build();
    }

    static software.amazon.awssdk.services.iot.model.PresignedUrlConfig getPresignedUrlConfig(ResourceModel model) {
        if(model == null || model.getPresignedUrlConfig() == null) {
            return null;
        }
        return PresignedUrlConfig.builder()
                .expiresInSec(model.getPresignedUrlConfig().getExpiresInSec().longValue())
                .roleArn(model.getPresignedUrlConfig().getRoleArn())
                .build();
    }

    static software.amazon.iot.jobtemplate.PresignedUrlConfig getPresignedUrlConfig(PresignedUrlConfig config) {
        if(config == null || config.expiresInSec() == null) {
            return null;
        }
        return software.amazon.iot.jobtemplate.PresignedUrlConfig.builder()
                .expiresInSec(config.expiresInSec().intValue())
                .roleArn(config.roleArn())
                .build();
    }

    static software.amazon.awssdk.services.iot.model.TimeoutConfig getTimeoutConfig(ResourceModel model) {
        if(model == null || model.getTimeoutConfig() == null) {
            return null;
        }
        return TimeoutConfig.builder()
                .inProgressTimeoutInMinutes(model.getTimeoutConfig().getInProgressTimeoutInMinutes().longValue())
                .build();
    }

    static software.amazon.iot.jobtemplate.TimeoutConfig getTimeoutConfig(TimeoutConfig config) {
        if(config == null || config.inProgressTimeoutInMinutes() == null) {
            return null;
        }
        return software.amazon.iot.jobtemplate.TimeoutConfig.builder()
                .inProgressTimeoutInMinutes(config.inProgressTimeoutInMinutes().intValue())
                .build();
    }

     static List<software.amazon.awssdk.services.iot.model.Tag> getTags(ResourceModel model) {
        if(model == null || model.getTags() == null || model.getTags().isEmpty()) {
            return null;
        }
        List<software.amazon.awssdk.services.iot.model.Tag> result = new ArrayList<>();
        for(software.amazon.iot.jobtemplate.Tag tag : model.getTags()) {
            result.add(software.amazon.awssdk.services.iot.model.Tag.builder().key(tag.getKey()).value(tag.getValue()).build());
        }
        return result;
    }
}
