package software.amazon.iot.jobtemplate;

import software.amazon.awssdk.services.iot.model.AbortConfig;
import software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig;
import software.amazon.awssdk.services.iot.model.PresignedUrlConfig;
import software.amazon.awssdk.services.iot.model.RateIncreaseCriteria;
import software.amazon.awssdk.services.iot.model.TimeoutConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Translator {

    static software.amazon.awssdk.services.iot.model.AbortConfig getAbortConfig(ResourceModel model) {
        if(model == null || model.getAbortConfig() == null) {
            return null;
        }
        software.amazon.iot.jobtemplate.AbortCriteria modelCriteria = model.getAbortConfig().getCriteriaList().get(0);

        software.amazon.awssdk.services.iot.model.AbortCriteria requestCriteria = software.amazon.awssdk.services.iot.model.AbortCriteria.builder()
                .action(modelCriteria.getAction())
                .failureType(modelCriteria.getFailureType())
                .thresholdPercentage(modelCriteria.getThresholdPercentage())
                .minNumberOfExecutedThings(modelCriteria.getMinNumberOfExecutedThings())
                .build();
        return  AbortConfig.builder()
                .criteriaList(requestCriteria)
                .build();
    }

    static software.amazon.iot.jobtemplate.AbortConfig getAbortConfig(AbortConfig config) {
        if(config == null || config.criteriaList().size() == 0 || config.criteriaList().get(0) == null) {
            return null;
        }
        software.amazon.awssdk.services.iot.model.AbortCriteria c = config.criteriaList().get(0);

        AbortCriteria criteria = AbortCriteria.builder()
                .action(c.actionAsString())
                .failureType(c.failureTypeAsString())
                .thresholdPercentage(c.thresholdPercentage())
                .minNumberOfExecutedThings(c.minNumberOfExecutedThings())
                .build();
        return software.amazon.iot.jobtemplate.AbortConfig.builder()
                .criteriaList(Collections.singletonList(criteria))
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
        if(config == null) {
            return null;
        }
        software.amazon.awssdk.services.iot.model.ExponentialRolloutRate e = config.exponentialRate();

        software.amazon.iot.jobtemplate.RateIncreaseCriteria rateIncreaseCriteria = software.amazon.iot.jobtemplate.RateIncreaseCriteria.builder()
                .numberOfNotifiedThings(e.rateIncreaseCriteria().numberOfNotifiedThings())
                .numberOfSucceededThings(e.rateIncreaseCriteria().numberOfSucceededThings())
                .build();

        ExponentialRolloutRate exponentialRolloutRate = ExponentialRolloutRate.builder()
                .baseRatePerMinute(e.baseRatePerMinute())
                .incrementFactor(e.incrementFactor())
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
        if(config == null) {
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
        if(config == null) {
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
