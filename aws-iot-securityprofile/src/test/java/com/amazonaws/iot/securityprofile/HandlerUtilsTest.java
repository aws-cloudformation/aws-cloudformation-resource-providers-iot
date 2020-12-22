package com.amazonaws.iot.securityprofile;

import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_ARN;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_NAME;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_TARGET_1;
import static com.amazonaws.iot.securityprofile.TestConstants.SECURITY_PROFILE_TARGET_2;
import static com.amazonaws.iot.securityprofile.TestConstants.TAGS_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_1_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.TAG_2_IOT;
import static com.amazonaws.iot.securityprofile.TestConstants.TARGET_ARNS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ListTargetsForSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.ListTargetsForSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

public class HandlerUtilsTest {

    private static final String NEXT_TOKEN = "testToken";

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    private IotClient iotClient;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        iotClient = IotClient.builder().build();
    }

    @Test
    void listTargetsForSecurityProfile_WithNextToken_VerifyPagination() {

        ListTargetsForSecurityProfileRequest expectedRequest1 = ListTargetsForSecurityProfileRequest.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .build();
        ListTargetsForSecurityProfileResponse response1 = ListTargetsForSecurityProfileResponse.builder()
                .securityProfileTargets(SECURITY_PROFILE_TARGET_1)
                .nextToken(NEXT_TOKEN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest1), any()))
                .thenReturn(response1);

        ListTargetsForSecurityProfileRequest expectedRequest2 = ListTargetsForSecurityProfileRequest.builder()
                .securityProfileName(SECURITY_PROFILE_NAME)
                .nextToken(NEXT_TOKEN)
                .build();
        ListTargetsForSecurityProfileResponse response2 = ListTargetsForSecurityProfileResponse.builder()
                .securityProfileTargets(SECURITY_PROFILE_TARGET_2)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest2), any()))
                .thenReturn(response2);

        Set<String> actualResponse = HandlerUtils.listTargetsForSecurityProfile(
                iotClient, proxy, SECURITY_PROFILE_NAME);
        assertThat(actualResponse).isEqualTo(TARGET_ARNS);
    }

    @Test
    void listTargetsForSecurityProfile_ApiThrowsException_BubbleUp() {

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(ThrottlingException.builder().build());
        assertThatThrownBy(() -> HandlerUtils.listTargetsForSecurityProfile(
                iotClient, proxy, SECURITY_PROFILE_NAME))
                .isInstanceOf(ThrottlingException.class);
    }

    @Test
    public void listTags_WithNextToken_VerifyPagination() {

        ListTagsForResourceRequest expectedRequest1 = ListTagsForResourceRequest.builder()
                .resourceArn(SECURITY_PROFILE_ARN)
                .build();
        ListTagsForResourceResponse response1 = ListTagsForResourceResponse.builder()
                .tags(TAG_1_IOT)
                .nextToken(NEXT_TOKEN)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest1), any()))
                .thenReturn(response1);

        ListTagsForResourceRequest expectedRequest2 = ListTagsForResourceRequest.builder()
                .resourceArn(SECURITY_PROFILE_ARN)
                .nextToken(NEXT_TOKEN)
                .build();
        ListTagsForResourceResponse listTagsForResourceResponse2 = ListTagsForResourceResponse.builder()
                .tags(TAG_2_IOT)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(eq(expectedRequest2), any()))
                .thenReturn(listTagsForResourceResponse2);

        Set<software.amazon.awssdk.services.iot.model.Tag> actualResponse =
                HandlerUtils.listTags(iotClient, proxy, SECURITY_PROFILE_ARN);
        assertThat(actualResponse).isEqualTo(TAGS_IOT);
    }

    @Test
    void listTags_ApiThrowsException_BubbleUp() {

        when(proxy.injectCredentialsAndInvokeV2(any(), any()))
                .thenThrow(LimitExceededException.builder().build());
        assertThatThrownBy(() -> HandlerUtils.listTags(
                iotClient, proxy, SECURITY_PROFILE_ARN))
                .isInstanceOf(LimitExceededException.class);
    }
}
