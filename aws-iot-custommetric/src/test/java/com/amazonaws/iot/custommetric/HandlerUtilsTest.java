package com.amazonaws.iot.custommetric;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.Arrays;
import java.util.List;

import static com.amazonaws.iot.custommetric.TestConstants.CUSTOM_METRIC_ARN;
import static com.amazonaws.iot.custommetric.TestConstants.SDK_MODEL_TAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HandlerUtilsTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private IotClient iotClient;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = mock(Logger.class);
        iotClient = IotClient.builder().build();
    }

    @Test
    public void listTags_WithNextToken_VerifyPagination() {

        ListTagsForResourceRequest expectedRequest1 = ListTagsForResourceRequest.builder()
                .resourceArn(CUSTOM_METRIC_ARN)
                .build();
        ListTagsForResourceResponse listTagsForResourceResponse1 = ListTagsForResourceResponse.builder()
                .tags(SDK_MODEL_TAG)
                .nextToken("testToken")
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest1), any()))
                .thenReturn(listTagsForResourceResponse1);

        ListTagsForResourceRequest expectedRequest2 = ListTagsForResourceRequest.builder()
                .resourceArn(CUSTOM_METRIC_ARN)
                .nextToken("testToken")
                .build();
        software.amazon.awssdk.services.iot.model.Tag tag2 = SDK_MODEL_TAG.toBuilder().key("key2").build();
        ListTagsForResourceResponse listTagsForResourceResponse2 = ListTagsForResourceResponse.builder()
                .tags(tag2)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest2), any()))
                .thenReturn(listTagsForResourceResponse2);

        List<Tag> currentTags = HandlerUtils.listTags(iotClient, proxy, CUSTOM_METRIC_ARN, logger);
        assertThat(currentTags).isEqualTo(Arrays.asList(SDK_MODEL_TAG, tag2));
    }
}
