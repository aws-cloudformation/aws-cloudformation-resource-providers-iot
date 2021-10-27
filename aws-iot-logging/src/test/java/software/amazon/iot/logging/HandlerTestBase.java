package software.amazon.iot.logging;

import org.mockito.Mock;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.GetV2LoggingOptionsResponse;
import software.amazon.awssdk.services.iot.model.SetV2LoggingOptionsRequest;
import software.amazon.awssdk.services.iot.model.SetV2LoggingOptionsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class HandlerTestBase {

    static final String ACCOUNT_ID = "123456789012";

    static final String DEFAULT_LOG_LEVEL = "DEBUG";

    static final String ROLE_ARN ="testRoleArn";

    static final GetV2LoggingOptionsRequest GET_REQUEST =
            GetV2LoggingOptionsRequest.builder().build();

    static final GetV2LoggingOptionsResponse GET_RESPONSE_WITH_ALL_LOGS_DISABLED =
            GetV2LoggingOptionsResponse.builder()
                    .defaultLogLevel(DEFAULT_LOG_LEVEL)
                    .roleArn(ROLE_ARN)
                    .disableAllLogs(true)
                    .build();

    static final GetV2LoggingOptionsResponse GET_RESPONSE_WITH_ALL_LOGS_ENABLED =
            GetV2LoggingOptionsResponse.builder()
                    .defaultLogLevel(DEFAULT_LOG_LEVEL)
                    .roleArn(ROLE_ARN)
                    .disableAllLogs(false)
                    .build();

    static final SetV2LoggingOptionsRequest SET_REQUEST =
            SetV2LoggingOptionsRequest.builder()
                    .defaultLogLevel(DEFAULT_LOG_LEVEL)
                    .roleArn(ROLE_ARN)
                    .disableAllLogs(false)
                    .build();

    static final SetV2LoggingOptionsRequest SET_REQUEST_FOR_UPDATE =
            SetV2LoggingOptionsRequest.builder()
                    .defaultLogLevel(DEFAULT_LOG_LEVEL)
                    .roleArn(ROLE_ARN)
                    .build();

    static final SetV2LoggingOptionsRequest SET_REQUEST_DISABLE_LOGS =
            SetV2LoggingOptionsRequest.builder()
                    .disableAllLogs(true)
                    .build();

    @Mock
    AmazonWebServicesClientProxy proxy;

    @Mock
    Logger logger;

    ResourceHandlerRequest<ResourceModel> createCfnRequest(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("doesn't matter")
                .awsAccountId(ACCOUNT_ID)
                .build();
    }

    ResourceModel createDefaultModel() {
        return ResourceModel.builder()
                .defaultLogLevel(DEFAULT_LOG_LEVEL)
                .roleArn(ROLE_ARN)
                .accountId(ACCOUNT_ID)
                .build();
    }


}
