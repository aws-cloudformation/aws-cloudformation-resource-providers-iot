package com.amazonaws.iot.securityprofile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.iot.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.iot.model.ListTargetsForSecurityProfileRequest;
import software.amazon.awssdk.services.iot.model.ListTargetsForSecurityProfileResponse;
import software.amazon.awssdk.services.iot.model.SecurityProfileTarget;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

public class HandlerUtils {

    static Set<String> listTargetsForSecurityProfile(
            IotClient iotClient,
            AmazonWebServicesClientProxy proxy,
            String securityProfileName) {

        String nextToken = null;
        Set<String> result = new HashSet<>();
        do {
            ListTargetsForSecurityProfileRequest listRequest = ListTargetsForSecurityProfileRequest.builder()
                    .securityProfileName(securityProfileName)
                    .nextToken(nextToken)
                    .build();
            ListTargetsForSecurityProfileResponse listResponse = proxy.injectCredentialsAndInvokeV2(
                    listRequest, iotClient::listTargetsForSecurityProfile);
            List<SecurityProfileTarget> securityProfileTargets = listResponse.securityProfileTargets();
            securityProfileTargets.forEach(target -> result.add(target.arn()));
            nextToken = listResponse.nextToken();
        } while (nextToken != null);

        return result;
    }

    static Set<software.amazon.awssdk.services.iot.model.Tag> listTags(
            IotClient iotClient,
            AmazonWebServicesClientProxy proxy,
            String resourceArn) {

        String nextToken = null;
        Set<Tag> result = new HashSet<>();
        do {
            ListTagsForResourceRequest listTagsRequest = ListTagsForResourceRequest.builder()
                    .resourceArn(resourceArn)
                    .nextToken(nextToken)
                    .build();
            ListTagsForResourceResponse listTagsForResourceResponse = proxy.injectCredentialsAndInvokeV2(
                        listTagsRequest, iotClient::listTagsForResource);
            result.addAll(listTagsForResourceResponse.tags());
            nextToken = listTagsForResourceResponse.nextToken();
        } while (nextToken != null);

        return result;
    }
}
