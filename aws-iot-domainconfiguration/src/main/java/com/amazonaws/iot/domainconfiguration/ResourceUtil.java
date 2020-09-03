package com.amazonaws.iot.domainconfiguration;

import software.amazon.awssdk.services.iot.model.AuthorizerConfig;

public final class ResourceUtil {

    /**
     * We wait 35 seconds between create/update and read to handle eventual consistency issues. Our canaries wait 31 seconds.
     * We wait 35 seconds between update(disable) and delete. For internal test accounts, we have a 30 second wait time
     * before a disabled AWS managed domain configuration can be deleted. This way we dont need additional logic to wait
     * between disable and delete for test for test accounts. Our canaries wait 35 seconds before deleting the resource.
     * */
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