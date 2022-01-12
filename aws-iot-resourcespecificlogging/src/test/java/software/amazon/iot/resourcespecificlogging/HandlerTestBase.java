package software.amazon.iot.resourcespecificlogging;

import org.mockito.Mock;
import software.amazon.awssdk.services.iot.model.DeleteV2LoggingLevelRequest;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsRequest;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsResponse;
import software.amazon.awssdk.services.iot.model.LogTargetConfiguration;
import software.amazon.awssdk.services.iot.model.SetV2LoggingLevelRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.List;

public class HandlerTestBase {

    static final String DEFAULT_LOG_LEVEL = "DEBUG";

    static final String TARGET_TYPE = "THING_GROUP";

    static final String TARGET_NAME = "TEST_THING_GROUP";

    static final List<LogTargetConfiguration> LOG_TARGET_CONFIGURATION_LIST = Collections.singletonList(
            LogTargetConfiguration.builder()
            .logLevel(DEFAULT_LOG_LEVEL)
            .logTarget(software.amazon.awssdk.services.iot.model.LogTarget.builder().targetName(TARGET_NAME).targetType(TARGET_TYPE).build())
            .build()
        );

    static final List<LogTargetConfiguration> LOG_TARGET_WITH_DEFAULT_TYPE_CONFIGURATION_LIST = Collections.singletonList(
            LogTargetConfiguration.builder()
                    .logLevel(DEFAULT_LOG_LEVEL)
                    .logTarget(software.amazon.awssdk.services.iot.model.LogTarget.builder().targetType("DEFAULT").build())
                    .build()
    );

    static final SetV2LoggingLevelRequest SET_REQUEST = SetV2LoggingLevelRequest.builder()
            .logLevel(DEFAULT_LOG_LEVEL)
            .logTarget(software.amazon.awssdk.services.iot.model.LogTarget.builder().targetName(TARGET_NAME).targetType(TARGET_TYPE).build())
            .build();

    static final ListV2LoggingLevelsRequest LIST_REQUEST = ListV2LoggingLevelsRequest.builder().maxResults(250).build();

    static final ListV2LoggingLevelsRequest LIST_DEFAULT_REQUEST = ListV2LoggingLevelsRequest.builder().build();

    static final ListV2LoggingLevelsResponse LIST_EMPTY_RESPONSE = ListV2LoggingLevelsResponse.builder()
            .logTargetConfigurations(Collections.emptyList())
            .nextToken(null)
            .build();

    static final ListV2LoggingLevelsResponse LIST_RESPONSE_WITH_DEFAULT_TYPE = ListV2LoggingLevelsResponse.builder()
            .logTargetConfigurations(LOG_TARGET_WITH_DEFAULT_TYPE_CONFIGURATION_LIST)
            .nextToken(null)
            .build();

    static final DeleteV2LoggingLevelRequest DELETE_REQUEST = DeleteV2LoggingLevelRequest.builder()
            .targetType(TARGET_TYPE)
            .targetName(TARGET_NAME)
            .build();

    static final ListV2LoggingLevelsResponse LIST_RESPONSE = ListV2LoggingLevelsResponse.builder()
            .logTargetConfigurations(LOG_TARGET_CONFIGURATION_LIST)
            .nextToken(null)
            .build();
    @Mock
    AmazonWebServicesClientProxy proxy;

    @Mock
    Logger logger;

    ResourceHandlerRequest<ResourceModel> createCfnRequest(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .logicalResourceIdentifier("doesn't matter")
                .build();
    }

    ResourceModel createDefaultModel() {
        return ResourceModel.builder()
                .targetType(TARGET_TYPE)
                .targetName(TARGET_NAME)
                .logLevel(DEFAULT_LOG_LEVEL)
                .build();
    }

    ResourceModel createDefaultModelWithTargetId() {
        return ResourceModel.builder()
                .targetType(TARGET_TYPE)
                .targetName(TARGET_NAME)
                .logLevel(DEFAULT_LOG_LEVEL)
                .targetId(TARGET_TYPE + ":" + TARGET_NAME)
                .build();
    }


}
