package software.amazon.iot.jobtemplate;

import org.checkerframework.checker.units.qual.A;
import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AbortCriteria;
import software.amazon.awssdk.services.iot.model.DescribeJobTemplateRequest;
import software.amazon.awssdk.services.iot.model.DescribeJobTemplateResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ResourceNotFoundException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static software.amazon.iot.jobtemplate.Translator.getAbortConfig;
import static software.amazon.iot.jobtemplate.Translator.getJobExecutionsRolloutConfig;
import static software.amazon.iot.jobtemplate.Translator.getPresignedUrlConfig;
import static software.amazon.iot.jobtemplate.Translator.getTimeoutConfig;

public class ReadHandler extends BaseHandler<CallbackContext> {
    private final static String OPERATION = "DescribeJobTemplate";
    private final IotClient iotClient;

    public ReadHandler() {
        this.iotClient = IotClient.create();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        final DescribeJobTemplateRequest describeJobTemplateRequest = DescribeJobTemplateRequest.builder()
                .jobTemplateId(model.getJobTemplateId())
                .build();

        try {
            DescribeJobTemplateResponse response = proxy.injectCredentialsAndInvokeV2(describeJobTemplateRequest, iotClient::describeJobTemplate);
            logger.log(String.format("%s [%s] read successfully", ResourceModel.TYPE_NAME, model.getJobTemplateId()));

            AbortConfig abortConfig = getAbortConfig(response.abortConfig());
            JobExecutionsRolloutConfig rolloutConfig = getJobExecutionsRolloutConfig(response.jobExecutionsRolloutConfig());
            PresignedUrlConfig presignedUrlConfig = getPresignedUrlConfig(response.presignedUrlConfig());
            TimeoutConfig timeoutConfig = getTimeoutConfig(response.timeoutConfig());


            return ProgressEvent.defaultSuccessHandler(ResourceModel.builder()
                    .jobTemplateArn(response.jobTemplateArn())
                    .jobTemplateId(response.jobTemplateId())
                    .abortConfig(abortConfig)
                    .description(response.description())
                    .document(response.document())
                    .documentSource(response.documentSource())
                    .jobExecutionsRolloutConfig(rolloutConfig)
                    .jobTemplateId(response.jobTemplateId())
                    .presignedUrlConfig(presignedUrlConfig)
                    .timeoutConfig(timeoutConfig)
                    .build());
        } catch (final ResourceNotFoundException e){
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getJobTemplateId());
        } catch (final InvalidRequestException e) {
            throw new CfnInvalidRequestException(e.getMessage(), e);
        } catch (final LimitExceededException e) {
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, e.getMessage());
        } catch (final InternalException e) {
            throw new CfnServiceInternalErrorException(OPERATION, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(OPERATION, e);
        } catch (final ServiceUnavailableException e) {
            throw new CfnGeneralServiceException(OPERATION, e);
        } catch (final UnauthorizedException e) {
            throw new CfnAccessDeniedException(OPERATION, e);
        }
    }
}
