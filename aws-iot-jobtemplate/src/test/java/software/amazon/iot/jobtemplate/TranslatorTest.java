package software.amazon.iot.jobtemplate;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TranslatorTest {
    final static String ACTION = "CANCEL";
    final static String FAILURE_TYPE = "FAILURE";
    final static String ROLE_ARN = "TEST";

    final static int minNumberOfExecutedThings = 1;
    final static double thresholdPercentage = 20.0;
    final static int numberOfNotifiedThings = 1;
    final static int numberOfSucceededThings = 2;
    final static double incrementFactor = 2.0;
    final static int baseRatePerMinute = 5;
    final static int maximumPerMinute = 1000;
    final int expiresInSec = 10;
    final int inProgressTimeoutInMinutes = 15;
    final static List<software.amazon.awssdk.services.iot.model.Tag> expectedTags;

    static {
        expectedTags = new ArrayList<>();
        expectedTags.add(software.amazon.awssdk.services.iot.model.Tag.builder().key("key1").value("value1").build());
        expectedTags.add(software.amazon.awssdk.services.iot.model.Tag.builder().key("key1").value("value1").build());
        expectedTags.add(software.amazon.awssdk.services.iot.model.Tag.builder().key("key1").value("value1").build());
    }

    @Test
    void getAbortConfigModelToSDK() {
        final AbortCriteria criteria = AbortCriteria.builder()
                .action(ACTION)
                .failureType(FAILURE_TYPE)
                .minNumberOfExecutedThings(minNumberOfExecutedThings)
                .thresholdPercentage(thresholdPercentage)
                .build();
        final AbortConfig abortConfig = AbortConfig.builder()
                .criteriaList(Collections.singletonList(criteria))
                .build();
        final ResourceModel model = ResourceModel.builder()
                .abortConfig(abortConfig)
                .build();

        final software.amazon.awssdk.services.iot.model.AbortConfig sdkAbortConfig = Translator.getAbortConfig(model);
        final software.amazon.awssdk.services.iot.model.AbortCriteria abortCriteria = sdkAbortConfig.criteriaList().get(0);

        assertNotNull(sdkAbortConfig);
        assertEquals(ACTION, abortCriteria.action().toString());
        assertEquals(FAILURE_TYPE, abortCriteria.failureTypeAsString());
        assertEquals(minNumberOfExecutedThings, abortCriteria.minNumberOfExecutedThings());
        assertEquals(thresholdPercentage, abortCriteria.thresholdPercentage());
    }

    @Test
    void getAbortConfigModelWithNullAbortConfig() {
        final ResourceModel model = ResourceModel.builder()
                .build();

        final software.amazon.awssdk.services.iot.model.AbortConfig sdkAbortConfig = Translator.getAbortConfig(model);

        assertNull(sdkAbortConfig);
    }

    @Test
    void getAbortConfigModelNull() {
        final ResourceModel model = null;

        final software.amazon.awssdk.services.iot.model.AbortConfig sdkAbortConfig = Translator.getAbortConfig(model);

        assertNull(sdkAbortConfig);
    }

    @Test
    void GetAbortConfigSDKtoModel() {
        final software.amazon.awssdk.services.iot.model.AbortCriteria c = software.amazon.awssdk.services.iot.model.AbortCriteria.builder()
                .action(ACTION)
                .failureType(FAILURE_TYPE)
                .minNumberOfExecutedThings(minNumberOfExecutedThings)
                .thresholdPercentage(thresholdPercentage)
                .build();
        final software.amazon.awssdk.services.iot.model.AbortConfig abortConfig = software.amazon.awssdk.services.iot.model.AbortConfig.builder()
                .criteriaList(c)
                .build();
        final AbortConfig modelAbortConfig = Translator.getAbortConfig(abortConfig);
        final AbortCriteria abortCriteria = modelAbortConfig.getCriteriaList().get(0);

        assertNotNull(modelAbortConfig);
        assertEquals(ACTION, abortCriteria.getAction());
        assertEquals(FAILURE_TYPE, abortCriteria.getFailureType());
        assertEquals(minNumberOfExecutedThings, abortCriteria.getMinNumberOfExecutedThings());
        assertEquals(thresholdPercentage, abortCriteria.getThresholdPercentage());
    }

    @Test
    void GetAbortConfigSDKWithNullCriteria() {
        final software.amazon.awssdk.services.iot.model.AbortConfig abortConfig = software.amazon.awssdk.services.iot.model.AbortConfig.builder()
                .criteriaList(Collections.singletonList(null))
                .build();
        final AbortConfig modelAbortConfig = Translator.getAbortConfig(abortConfig);

        assertNull(modelAbortConfig);
    }

    @Test
    void GetAbortConfigSDKNull() {
        final software.amazon.awssdk.services.iot.model.AbortConfig abortConfig = null;

        final AbortConfig modelAbortConfig = Translator.getAbortConfig(abortConfig);

        assertNull(modelAbortConfig);
    }

    @Test
    void getJobExecutionsRolloutConfigModelToSDK() {
        final RateIncreaseCriteria rateIncreaseCriteria = RateIncreaseCriteria.builder()
                .numberOfNotifiedThings(numberOfNotifiedThings)
                .numberOfSucceededThings(numberOfSucceededThings)
                .build();
        final ExponentialRolloutRate exponentialRolloutRate = ExponentialRolloutRate.builder()
                .rateIncreaseCriteria(rateIncreaseCriteria)
                .incrementFactor(incrementFactor)
                .baseRatePerMinute(baseRatePerMinute)
                .build();
        final JobExecutionsRolloutConfig jobExecutionsRolloutConfig = JobExecutionsRolloutConfig.builder()
                .exponentialRolloutRate(exponentialRolloutRate)
                .maximumPerMinute(maximumPerMinute)
                .build();
        final ResourceModel model = ResourceModel.builder()
                .jobExecutionsRolloutConfig(jobExecutionsRolloutConfig)
                .build();
        final software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig sdkConfig = Translator.getJobExecutionsRolloutConfig(model);

        assertNotNull(sdkConfig);
        assertEquals(numberOfNotifiedThings, sdkConfig.exponentialRate().rateIncreaseCriteria().numberOfNotifiedThings());
        assertEquals(numberOfSucceededThings, sdkConfig.exponentialRate().rateIncreaseCriteria().numberOfSucceededThings());
        assertEquals(incrementFactor, sdkConfig.exponentialRate().incrementFactor());
        assertEquals(baseRatePerMinute, sdkConfig.exponentialRate().baseRatePerMinute());
        assertEquals(maximumPerMinute, sdkConfig.maximumPerMinute());
    }

    @Test
    void getJobExecutionsRolloutConfigModelWithNullConfig() {
        final ResourceModel model = ResourceModel.builder()
                .jobExecutionsRolloutConfig(null)
                .build();
        final software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig sdkConfig = Translator.getJobExecutionsRolloutConfig(model);

        assertNull(sdkConfig);
    }

    @Test
    void getJobExecutionsRolloutConfigModelNull() {
        final ResourceModel model = null;
        final software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig sdkConfig = Translator.getJobExecutionsRolloutConfig(model);

        assertNull(sdkConfig);
    }

    @Test
    void getJobExecutionsRolloutConfigSDKtoModel() {
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
        final JobExecutionsRolloutConfig modelConfig = Translator.getJobExecutionsRolloutConfig(jobExecutionsRolloutConfig);

        assertNotNull(modelConfig);
        assertEquals(numberOfNotifiedThings, modelConfig.getExponentialRolloutRate().getRateIncreaseCriteria().getNumberOfNotifiedThings());
        assertEquals(numberOfSucceededThings, modelConfig.getExponentialRolloutRate().getRateIncreaseCriteria().getNumberOfSucceededThings());
        assertEquals(incrementFactor, modelConfig.getExponentialRolloutRate().getIncrementFactor());
        assertEquals(baseRatePerMinute, modelConfig.getExponentialRolloutRate().getBaseRatePerMinute());
        assertEquals(maximumPerMinute, modelConfig.getMaximumPerMinute());
    }

    @Test
    void getJobExecutionsRolloutConfigSDKWithNullConfig() {
        final software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig jobExecutionsRolloutConfig = null;

        final JobExecutionsRolloutConfig modelConfig = Translator.getJobExecutionsRolloutConfig(jobExecutionsRolloutConfig);

        assertNull(modelConfig);
    }

    @Test
    void getPresignedUrlConfigModelToSDK() {
        final PresignedUrlConfig presignedUrlConfig = PresignedUrlConfig.builder()
                .expiresInSec(expiresInSec)
                .roleArn(ROLE_ARN)
                .build();
        final ResourceModel model = ResourceModel.builder()
                .presignedUrlConfig(presignedUrlConfig)
                .build();
        final software.amazon.awssdk.services.iot.model.PresignedUrlConfig sdkConfig = Translator.getPresignedUrlConfig(model);

        assertNotNull(sdkConfig);
        assertEquals(expiresInSec, sdkConfig.expiresInSec().intValue());
    }

    @Test
    void getPresignedUrlConfigModelWithNullConfig() {
        final ResourceModel model = ResourceModel.builder()
                .presignedUrlConfig(null)
                .build();
        final software.amazon.awssdk.services.iot.model.PresignedUrlConfig sdkConfig = Translator.getPresignedUrlConfig(model);

        assertNull(sdkConfig);
    }

    @Test
    void getPresignedUrlConfigModelNull() {
        final ResourceModel model = null;
        final software.amazon.awssdk.services.iot.model.PresignedUrlConfig sdkConfig = Translator.getPresignedUrlConfig(model);

        assertNull(sdkConfig);
    }

    @Test
    void getPresignedUrlConfigSDKtoModel() {
        final software.amazon.awssdk.services.iot.model.PresignedUrlConfig presignedUrlConfig = software.amazon.awssdk.services.iot.model.PresignedUrlConfig.builder()
                .expiresInSec((long)expiresInSec)
                .roleArn(ROLE_ARN)
                .build();
        final PresignedUrlConfig modelConfig = Translator.getPresignedUrlConfig(presignedUrlConfig);

        assertNotNull(modelConfig);
        assertEquals(expiresInSec, modelConfig.getExpiresInSec());
        assertEquals(ROLE_ARN, modelConfig.getRoleArn());
    }

    @Test
    void getPresignedUrlConfigSDKNull() {
        final software.amazon.awssdk.services.iot.model.PresignedUrlConfig presignedUrlConfig = null;
        final PresignedUrlConfig modelConfig = Translator.getPresignedUrlConfig(presignedUrlConfig);

        assertNull(modelConfig);
    }

    @Test
    void getTimeoutConfigModelToSDK() {
        final TimeoutConfig timeoutConfig = TimeoutConfig.builder()
                .inProgressTimeoutInMinutes(inProgressTimeoutInMinutes)
                .build();
        final ResourceModel resourceModel = ResourceModel.builder()
                .timeoutConfig(timeoutConfig)
                .build();
        final software.amazon.awssdk.services.iot.model.TimeoutConfig sdkConfig = Translator.getTimeoutConfig(resourceModel);

        assertNotNull(sdkConfig);
        assertEquals(inProgressTimeoutInMinutes, sdkConfig.inProgressTimeoutInMinutes());
    }

    @Test
    void getTimeoutConfigModelWithNullConfig() {
        final ResourceModel resourceModel = ResourceModel.builder()
                .timeoutConfig(null)
                .build();
        final software.amazon.awssdk.services.iot.model.TimeoutConfig sdkConfig = Translator.getTimeoutConfig(resourceModel);

        assertNull(sdkConfig);
    }

    @Test
    void getTimeoutConfigModelNull() {
        final ResourceModel resourceModel = null;

        final software.amazon.awssdk.services.iot.model.TimeoutConfig sdkConfig = Translator.getTimeoutConfig(resourceModel);

        assertNull(sdkConfig);
    }

    @Test
    void getTimeoutConfigSDKtoModel() {
        final software.amazon.awssdk.services.iot.model.TimeoutConfig timeoutConfig = software.amazon.awssdk.services.iot.model.TimeoutConfig.builder()
                .inProgressTimeoutInMinutes((long)inProgressTimeoutInMinutes)
                .build();
        final TimeoutConfig modelConfig = Translator.getTimeoutConfig(timeoutConfig);

        assertNotNull(modelConfig);
        assertEquals(inProgressTimeoutInMinutes, modelConfig.getInProgressTimeoutInMinutes());
    }

    @Test
    void getTimeoutConfigSDKNull() {
        final software.amazon.awssdk.services.iot.model.TimeoutConfig timeoutConfig = null;

        final TimeoutConfig modelconfig = Translator.getTimeoutConfig(timeoutConfig);

        assertNull(modelconfig);
    }

    @Test
    void getTags() {
        final Set<Tag> tags = new HashSet<>();
        tags.add(Tag.builder().key("key1").value("value1").build());
        tags.add(Tag.builder().key("key2").value("value2").build());
        tags.add(Tag.builder().key("key3").value("value3").build());

        final ResourceModel model = ResourceModel.builder()
                .tags(tags)
                .build();
        final List<software.amazon.awssdk.services.iot.model.Tag> result = Translator.getTags(model);

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertTrue(result.containsAll(expectedTags));
    }

    @Test
    void getTagsEmpty() {
        final Set<Tag> tags = new HashSet<>();

        final ResourceModel model = ResourceModel.builder()
                .tags(tags)
                .build();
        final List<software.amazon.awssdk.services.iot.model.Tag> result = Translator.getTags(model);

        assertNull(result);
    }

    @Test
    void getTagsNullTags() {
        final ResourceModel model = ResourceModel.builder()
                .tags(null)
                .build();
        final List<software.amazon.awssdk.services.iot.model.Tag> result = Translator.getTags(model);

        assertNull(result);
    }
}
