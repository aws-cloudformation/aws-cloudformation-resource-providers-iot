package software.amazon.iot.resourcespecificlogging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsRequest;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsResponse;
import software.amazon.awssdk.services.iot.model.LogTargetConfiguration;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandlerUtilsTest {
    @Mock
    private AmazonWebServicesClientProxy proxy;

    private IotClient iotClient;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        iotClient = IotClient.builder().build();
    }

    @Test
    public void listTargetWithNextToken () {
        ListV2LoggingLevelsRequest expectedListRequest1 = ListV2LoggingLevelsRequest.builder().maxResults(250).build();

        List<LogTargetConfiguration> expectedListLogTargetConfiguration1 = Collections.singletonList(
                LogTargetConfiguration.builder()
                        .logLevel("DEBUG")
                        .logTarget(software.amazon.awssdk.services.iot.model.LogTarget.builder().targetName("ThingGroup-1").targetType("THING_GROUP").build())
                        .build()
        );

        ListV2LoggingLevelsResponse expectedListResponse1 = ListV2LoggingLevelsResponse.builder()
                .logTargetConfigurations(expectedListLogTargetConfiguration1)
                .nextToken("testToken")
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedListRequest1), any()))
                .thenReturn(expectedListResponse1);

        ListV2LoggingLevelsRequest expectedListRequest2 = ListV2LoggingLevelsRequest.builder()
                .maxResults(250)
                .nextToken("testToken")
                .build();

        List<LogTargetConfiguration> expectedListLogTargetConfiguration2 = Collections.singletonList(
                LogTargetConfiguration.builder()
                        .logLevel("ERROR")
                        .logTarget(software.amazon.awssdk.services.iot.model.LogTarget.builder().targetName("ThingGroup-2").targetType("THING_GROUP").build())
                        .build()
        );

        ListV2LoggingLevelsResponse expectedListResponse2 = ListV2LoggingLevelsResponse.builder()
                .logTargetConfigurations(expectedListLogTargetConfiguration2)
                .nextToken(null)
                .build();

        when(proxy.injectCredentialsAndInvokeV2(eq(expectedListRequest2), any()))
                .thenReturn(expectedListResponse2);

        String expectedLogLevelForTarget = "ERROR";

        assertThat(expectedLogLevelForTarget.equals(HandlerUtils.getLoggingLevelForTarget("THING_GROUP", "ThingGroup-2", proxy, iotClient)));
    }

}