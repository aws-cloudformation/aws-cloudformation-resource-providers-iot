package com.amazonaws.iot.topicruledestination;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.services.iot.IotClient;

public class ClientBuilder {
    private static volatile IotClient iotClient;

    static IotClient getClient() {
        if (iotClient != null) {
            return iotClient;
        }

        synchronized (ClientBuilder.class) {
            iotClient = IotClient.builder()
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .retryPolicy(RetryPolicy.builder().numRetries(3).build())
                            .build())
                    .build();
            return iotClient;
        }
    }
}