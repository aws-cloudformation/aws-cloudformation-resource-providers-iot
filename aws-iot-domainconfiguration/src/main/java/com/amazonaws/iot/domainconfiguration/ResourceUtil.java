package com.amazonaws.iot.domainconfiguration;

import software.amazon.awssdk.services.iot.model.AuthorizerConfig;

public final class ResourceUtil {

    public static final int DELAY_CONSTANT = 35;
    public static final int MAX_RETRIES = 3;

    /**
     * Convert the resource model's authorizer config to the type expected by the SDK
     * @param model The resource model
     * @return A converted AuthorizerConfig or null if none was present in the model
     */
    public static AuthorizerConfig getSdkAuthorizerConfig(ResourceModel model) {
        final com.amazonaws.iot.domainconfiguration.AuthorizerConfig config = model.getAuthorizerConfig();
        if (config != null) {
            return AuthorizerConfig.builder()
                    .allowAuthorizerOverride(config.getAllowAuthorizerOverride())
                    .defaultAuthorizerName(config.getDefaultAuthorizerName())
                    .build();
        }
        return null;
    }

    /**
     * Convert the SDK's authorizer config to the type expected by CloudFormation
     * @param config The SDK AuthorizerConfig
     * @return A converted AuthorizerConfig or null if none was present in the response
     */
    public static com.amazonaws.iot.domainconfiguration.AuthorizerConfig getResourceModelAuthorizerConfig(AuthorizerConfig config) {
        if (config != null) {
            return com.amazonaws.iot.domainconfiguration.AuthorizerConfig.builder()
                    .allowAuthorizerOverride(config.allowAuthorizerOverride())
                    .defaultAuthorizerName(config.defaultAuthorizerName())
                    .build();
        }
        return null;
    }

}
