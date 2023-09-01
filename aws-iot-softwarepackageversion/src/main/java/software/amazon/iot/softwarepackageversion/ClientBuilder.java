package software.amazon.iot.softwarepackageversion;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static IotClient getClient() {
        return IotClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
