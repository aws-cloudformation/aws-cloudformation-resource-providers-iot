package com.amazonaws.iot.policy;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iot.model.DeletePolicyResponse;
import software.amazon.awssdk.services.iot.model.DeletePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsResponse;
import software.amazon.awssdk.services.iot.model.PolicyVersion;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeletePolicy";
    private static final String CALL_GRAPH = "AWS-IoT-Policy::Delete";
    private Logger logger;

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<IotClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        if (model.getPolicyName() == null) {
            model.setPolicyName(model.getId());
        }

        final String policyName = model.getPolicyName();

        //Before the deletion on default version of policy, delete other all non-default version
        try {
            List<PolicyVersion> versions = getVersionsSortedByAge(proxyClient, policyName);
            for (PolicyVersion version : versions) {
                if (!version.isDefaultVersion()) {
                    DeletePolicyVersionRequest deletePolicyVersionRequest = DeletePolicyVersionRequest.builder()
                            .policyName(policyName)
                            .policyVersionId(version.versionId())
                            .build();
                    proxyClient.injectCredentialsAndInvokeV2(deletePolicyVersionRequest, proxyClient.client()::deletePolicyVersion);
                    }
                }
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("%s [%s] Does Not Exist", ResourceModel.TYPE_NAME, policyName));
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
        }

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate(CALL_GRAPH, proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deleteResource)
                                .stabilize(this::stabilizedOnDelete)
                                .done(response -> ProgressEvent.defaultSuccessHandler(null))
                );

    }


    private Boolean stabilizedOnDelete(DeletePolicyRequest request, DeletePolicyResponse response, ProxyClient<IotClient> proxyClient, ResourceModel model, CallbackContext callbackContext) {
        try {
            proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::getPolicy);
            return false;
        } catch (ResourceNotFoundException e) {
            logger.log(String.format("%s [%s] deletion has stabilized.", ResourceModel.TYPE_NAME, request.policyName()));
            return true;
        }
    }

    private DeletePolicyResponse deleteResource(DeletePolicyRequest request, ProxyClient<IotClient> proxyClient) {
        try {
            DeletePolicyResponse response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deletePolicy);
            logger.log(String.format("%s [%s] successfully deleted.", ResourceModel.TYPE_NAME, request.policyName()));
            return response;
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(e, OPERATION, request.policyName());
        }
    }

    private List<PolicyVersion> getVersionsSortedByAge(ProxyClient<IotClient> proxyClient, String policyName) {
        ListPolicyVersionsRequest listPolicyVersionsRequest = ListPolicyVersionsRequest.builder()
                .policyName(policyName)
                .build();
        final ListPolicyVersionsResponse listPolicyVersionsResponse = proxyClient.injectCredentialsAndInvokeV2(
                listPolicyVersionsRequest,
                proxyClient.client()::listPolicyVersions);

        return listPolicyVersionsResponse.policyVersions().stream()
                .sorted(Comparator.comparing(PolicyVersion::versionId))
                .collect(Collectors.toList());
    }

}
