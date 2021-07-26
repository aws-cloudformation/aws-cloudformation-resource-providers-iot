package com.amazonaws.iot.policy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Map;

import static org.mockito.Mockito.mock;

public class PolicyTestBase {

    protected final static String REQUEST_TOKEN = "RequestToken";
    protected final static String LOGICAL_ID = "MyPolicy";
    protected final static String POLICY_NAME = "SamplePolicyName";
    protected final static String POLICY_DOCUMENT = "{\\\"Version\\\": \\\"2012-10-17\\\",\\\"Statement\\\": [{\\\"Effect\\\": \\\"Allow\\\",\\\"Action\\\": [\\\"*\\\"],\\\"Resource\\\": [\\\"*\\\"]}]}";
    protected final static String POLICY_ARN = "arn:aws:iot:us-east-1:0123456789:policy/mypolicy";
    protected final static String POLICY_ID = "5066f1b6712ce9d2a1e56399771649a272d6a921762fead080e24fe52f24e042";
    protected IotClient iotClient;


    @BeforeEach
    public void setup() {
        iotClient = mock(IotClient.class);
    }



    protected static ResourceModel.ResourceModelBuilder defaultModelBuilder() {
        return ResourceModel.builder()
                .policyName(POLICY_NAME)
                .policyDocument(convertPolicyDocumentJSONStringToMap(POLICY_DOCUMENT))
                .arn(POLICY_ARN)
                .id(POLICY_ID);
    }

    protected final static CreatePolicyResponse DEFAULT_CREATE_POLICY_RESPONSE = CreatePolicyResponse.builder()
            .policyName(POLICY_NAME)
            .policyDocument(POLICY_DOCUMENT)
            .policyArn(POLICY_ARN)
            .build();

    protected static ResourceHandlerRequest.ResourceHandlerRequestBuilder<ResourceModel> defaultRequestBuilder(ResourceModel model) {
        return ResourceHandlerRequest.<ResourceModel>builder()
                .clientRequestToken(REQUEST_TOKEN)
                .logicalResourceIdentifier(LOGICAL_ID)
                .desiredResourceState(model);
    }

    protected final static ResourceModel DEFAULT_RESOURCE_MODEL = ResourceModel.builder()
            .policyName(POLICY_NAME)
            .arn(POLICY_ARN)
            .build();

    private static Map<String, Object> convertPolicyDocumentJSONStringToMap(final String policyDocument) {
        ObjectMapper policyDocumentMapper = new ObjectMapper();
        try {
            TypeReference<Map<String,Object>> typeRef
                    = new TypeReference<Map<String,Object>>() {};
            return policyDocumentMapper.readValue(policyDocument,  typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


}
