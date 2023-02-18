package com.amazonaws.iot.policy;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.CreatePolicyVersionResponse;
import software.amazon.awssdk.services.iot.model.DeletePolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPolicyVersionsResponse;
import software.amazon.awssdk.services.iot.model.PolicyVersion;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.SetDefaultPolicyVersionRequest;
import software.amazon.awssdk.services.iot.model.VersionsLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.amazonaws.iot.policy.Translator.translateToCreateVersionRequest;

public class UpdateHandler extends BaseHandlerStd {
    private static final String OPERATION = "UpdatePolicy";
    private static final String CALL_GRAPH = "AWS-IoT-Policy::Update";

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel newModel = request.getDesiredResourceState();
        final ResourceModel prevModel = request.getPreviousResourceState();

        if(StringUtils.isNullOrEmpty(newModel.getPolicyName())) {
            newModel.setPolicyName(newModel.getId());
        }

        final String policyName = newModel.getPolicyName();

        if (!prevModel.getPolicyDocument().equals(newModel.getPolicyDocument())) {

            // For consistency we always set the latest version as the default version of Policy.
            CreatePolicyVersionRequest createPolicyVersionRequest = translateToCreateVersionRequest(newModel);
            try {
                proxyClient.injectCredentialsAndInvokeV2(createPolicyVersionRequest, proxyClient.client()::createPolicyVersion);
            } catch (ResourceNotFoundException e) {
                logger.log(String.format("%s [%s] Does Not Exist", ResourceModel.TYPE_NAME, policyName));
                return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotFound);
            } catch (VersionsLimitExceededException e) {
                final List<PolicyVersion> versions = getVersionsSortedByAge(proxyClient, policyName);

                // For consistency we always delete the oldest version, so check now if that is the default currently
                if (versions.get(0).isDefaultVersion()) {
                    SetDefaultPolicyVersionRequest setDefaultPolicyVersionRequest = SetDefaultPolicyVersionRequest.builder()
                            .policyName(policyName)
                            .policyVersionId(versions.get(versions.size() - 1).versionId())
                            .build();
                    proxyClient.injectCredentialsAndInvokeV2(setDefaultPolicyVersionRequest, proxyClient.client()::setDefaultPolicyVersion);
                }

                DeletePolicyVersionRequest deletePolicyVersionRequest = DeletePolicyVersionRequest.builder()
                        .policyName(policyName)
                        .policyVersionId(versions.get(0).versionId())
                        .build();
                proxyClient.injectCredentialsAndInvokeV2(deletePolicyVersionRequest, proxyClient.client()::deletePolicyVersion);
                proxyClient.injectCredentialsAndInvokeV2(createPolicyVersionRequest, proxyClient.client()::createPolicyVersion);
            } catch (IotException e) {
                throw Translator.translateIotExceptionToHandlerException(e, OPERATION, createPolicyVersionRequest.policyName());
            }
        }

        return ProgressEvent.defaultSuccessHandler(newModel);
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
