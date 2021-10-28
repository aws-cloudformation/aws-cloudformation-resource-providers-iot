package software.amazon.iot.resourcespecificlogging;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsRequest;
import software.amazon.awssdk.services.iot.model.ListV2LoggingLevelsResponse;
import software.amazon.awssdk.services.iot.model.LogTargetConfiguration;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

public class HandlerUtils {

    public static String getLoggingLevelForTarget(String targetType, String targetName, AmazonWebServicesClientProxy proxy, IotClient iotClient) {
        /**
         *  skyfall doesn't have describe api, we need to use listLoggingLevels to featch a specific log level for a target
         *  return null if no log level set for target
         */
        String nextToken = null;

        do {
            ListV2LoggingLevelsRequest listRequest = ListV2LoggingLevelsRequest.builder()
                    .nextToken(nextToken)
                    .maxResults(250)
                    .build();
            ListV2LoggingLevelsResponse listResponse = proxy.injectCredentialsAndInvokeV2(listRequest, iotClient::listV2LoggingLevels);

            for(LogTargetConfiguration logTargetConfiguration : listResponse.logTargetConfigurations()) {
                if(logTargetConfiguration.logTarget().targetTypeAsString().equals(targetType) && logTargetConfiguration.logTarget().targetName().equals(targetName)) {
                    return logTargetConfiguration.logLevelAsString();
                }
            }
        } while (nextToken != null);

        return null;
    }

    public static String targetIdBuilder(String targetType, String targetName) {
        // we have set the targetType and targetName required value in jason schema so we don't need to check they're non-null

        return targetType + ":" + targetName;
    }
}
