package software.amazon.iot.softwarepackageversion;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.CreatePackageRequest;
import software.amazon.awssdk.services.iot.model.GetPackageRequest;
import software.amazon.awssdk.services.iot.model.IotException;
import software.amazon.awssdk.services.iot.model.ListPackageVersionsRequest;
import software.amazon.awssdk.services.iot.model.ListPackageVersionsResponse;
import software.amazon.awssdk.services.iot.model.ListPackagesRequest;
import software.amazon.awssdk.services.iot.model.ListPackagesResponse;
import software.amazon.awssdk.services.iot.model.PackageSummary;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

/**
 * API Calls for ListHandler:
 * ListPackageVersions: To list all PackageVersions in an account
 */
public class ListHandler extends BaseHandlerStd {

    private static final String OPERATION = "ListSoftwarePackageVersions";
    private static final String DEFAULT_PACKAGE_NAME = "cloudformation-default-package";


    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<IotClient> proxyClient,
            final Logger logger) {
        final ResourceModel resourceModel = request.getDesiredResourceState();
        String packageName = resourceModel.getPackageName();

        try {
            // For contract test scenario only
            if (packageName == null || packageName.isEmpty()) {
                packageName = DEFAULT_PACKAGE_NAME;
                final GetPackageRequest getPackageRequest = GetPackageRequest.builder()
                        .packageName(packageName)
                        .build();
                try {
                    proxy.injectCredentialsAndInvokeV2(getPackageRequest, proxyClient.client()::getPackage);
                } catch (final ResourceNotFoundException e) {
                    final CreatePackageRequest createPackageRequest = CreatePackageRequest.builder()
                            .packageName(DEFAULT_PACKAGE_NAME)
                            .build();

                    proxy.injectCredentialsAndInvokeV2(createPackageRequest, proxyClient.client()::createPackage);
                }
            }
            final ListPackageVersionsRequest listPackageVersionsRequest = Translator.translateToListRequest(packageName, request.getNextToken());
            ListPackageVersionsResponse listPackageVersionsResponse = proxy.injectCredentialsAndInvokeV2(
                    listPackageVersionsRequest,
                    proxyClient.client()::listPackageVersions
            );
            String nextToken = listPackageVersionsResponse.nextToken();
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(Translator.translateFromListResponse(listPackageVersionsResponse))
                    .nextToken(nextToken)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (IotException e) {
            throw Translator.translateIotExceptionToHandlerException(packageName, OPERATION, e);
        }
    }
}
