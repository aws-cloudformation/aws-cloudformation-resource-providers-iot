package software.amazon.iot.jobtemplate;

import org.mockito.Mock;
import software.amazon.awssdk.services.iot.model.CreateJobTemplateResponse;
import software.amazon.awssdk.services.iot.model.DescribeJobTemplateResponse;
import software.amazon.awssdk.services.iot.model.Job;
import software.amazon.awssdk.services.iot.model.JobTemplateSummary;
import software.amazon.awssdk.services.iot.model.ListJobTemplatesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.ArrayList;
import java.util.List;

public class HandlerTestBase {
    final static String ACTION = "CANCEL";
    final static String FAILURE_TYPE = "FAILURE";
    final static String ROLE_ARN = "TEST";
    final static String JOB_TEMPLATE_ID = "JobTemplate-Test";
    final static String JOB_TEMPLATE_ARN = "test-jobtemplate-arn";
    final static String JOB_ARN = "test-job-arn";
    final static String JOB_TEMPLATE_DESCRIPTION = "test";

    final static int minNumberOfExecutedThings = 1;
    final static double thresholdPercentage = 20.0;
    final static int numberOfNotifiedThings = 1;
    final static int numberOfSucceededThings = 2;
    final static double incrementFactor = 2.0;
    final static int baseRatePerMinute = 5;
    final static int maximumPerMinute = 1000;
    final int expiresInSec = 10;
    final int inProgressTimeoutInMinutes = 15;

    @Mock
    AmazonWebServicesClientProxy proxy;

    @Mock
    Logger logger;

    ListJobTemplatesResponse getListResponse() {
        List<JobTemplateSummary> jobTemplates = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            JobTemplateSummary summary = JobTemplateSummary.builder()
                    .jobTemplateArn(JOB_TEMPLATE_ARN + i)
                    .jobTemplateId(JOB_TEMPLATE_ID + i)
                    .build();
            jobTemplates.add(summary);
        }
        return ListJobTemplatesResponse.builder()
                .jobTemplates(jobTemplates)
                .build();
    }

    CreateJobTemplateResponse getCreateResponse() {
        return CreateJobTemplateResponse.builder()
                .jobTemplateArn(JOB_TEMPLATE_ARN)
                .jobTemplateId(JOB_TEMPLATE_ID)
                .build();
    }

    DescribeJobTemplateResponse getDescribeResponse() {
        final software.amazon.awssdk.services.iot.model.AbortCriteria c = software.amazon.awssdk.services.iot.model.AbortCriteria.builder()
                .action(ACTION)
                .failureType(FAILURE_TYPE)
                .minNumberOfExecutedThings(minNumberOfExecutedThings)
                .thresholdPercentage(thresholdPercentage)
                .build();
        final software.amazon.awssdk.services.iot.model.AbortConfig abortConfig = software.amazon.awssdk.services.iot.model.AbortConfig.builder()
                .criteriaList(c)
                .build();
        final software.amazon.awssdk.services.iot.model.RateIncreaseCriteria rateIncreaseCriteria = software.amazon.awssdk.services.iot.model.RateIncreaseCriteria.builder()
                .numberOfNotifiedThings(numberOfNotifiedThings)
                .numberOfSucceededThings(numberOfSucceededThings)
                .build();
        final software.amazon.awssdk.services.iot.model.ExponentialRolloutRate exponentialRolloutRate = software.amazon.awssdk.services.iot.model.ExponentialRolloutRate.builder()
                .rateIncreaseCriteria(rateIncreaseCriteria)
                .incrementFactor(incrementFactor)
                .baseRatePerMinute(baseRatePerMinute)
                .build();
        final software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig jobExecutionsRolloutConfig = software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig.builder()
                .exponentialRate(exponentialRolloutRate)
                .maximumPerMinute(maximumPerMinute)
                .build();
        final software.amazon.awssdk.services.iot.model.PresignedUrlConfig presignedUrlConfig = software.amazon.awssdk.services.iot.model.PresignedUrlConfig.builder()
                .expiresInSec((long)expiresInSec)
                .roleArn(ROLE_ARN)
                .build();
        final software.amazon.awssdk.services.iot.model.TimeoutConfig timeoutConfig = software.amazon.awssdk.services.iot.model.TimeoutConfig.builder()
                .inProgressTimeoutInMinutes((long)inProgressTimeoutInMinutes)
                .build();

        return DescribeJobTemplateResponse.builder()
                .jobTemplateId(JOB_TEMPLATE_ID)
                .jobTemplateArn(JOB_TEMPLATE_ARN)
                .description(JOB_TEMPLATE_DESCRIPTION)
                .abortConfig(abortConfig)
                .jobExecutionsRolloutConfig(jobExecutionsRolloutConfig)
                .presignedUrlConfig(presignedUrlConfig)
                .timeoutConfig(timeoutConfig)
                .build();
    }
}
