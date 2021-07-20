package software.amazon.iot.jobtemplate;

import software.amazon.awssdk.services.iot.IotClient;
import software.amazon.awssdk.services.iot.model.AbortConfig;
import software.amazon.awssdk.services.iot.model.CreateJobTemplateRequest;
import software.amazon.awssdk.services.iot.model.CreateJobTemplateResponse;
import software.amazon.awssdk.services.iot.model.InternalException;
import software.amazon.awssdk.services.iot.model.InvalidRequestException;
import software.amazon.awssdk.services.iot.model.JobExecutionsRolloutConfig;
import software.amazon.awssdk.services.iot.model.LimitExceededException;
import software.amazon.awssdk.services.iot.model.PresignedUrlConfig;
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.iot.model.ServiceUnavailableException;
import software.amazon.awssdk.services.iot.model.Tag;
import software.amazon.awssdk.services.iot.model.ThrottlingException;
import software.amazon.awssdk.services.iot.model.TimeoutConfig;
import software.amazon.awssdk.services.iot.model.UnauthorizedException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static software.amazon.iot.jobtemplate.Translator.getAbortConfig;
import static software.amazon.iot.jobtemplate.Translator.getJobExecutionsRolloutConfig;
import static software.amazon.iot.jobtemplate.Translator.getPresignedUrlConfig;
import static software.amazon.iot.jobtemplate.Translator.getTags;
import static software.amazon.iot.jobtemplate.Translator.getTimeoutConfig;

public class CreateHandler extends BaseHandler<CallbackContext> {
    private final static String OPERATION = "CreateJobTemplate";
    private final IotClient iotClient;

    public CreateHandler() {
        this.iotClient = IotClient.create();
    }


    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        AbortConfig abortConfig = getAbortConfig(model);
        JobExecutionsRolloutConfig rolloutConfig = getJobExecutionsRolloutConfig(model);
        PresignedUrlConfig presignedUrlConfig = getPresignedUrlConfig(model);
        TimeoutConfig timeoutConfig = getTimeoutConfig(model);
        List<Tag> tags = getTags(model);


        final CreateJobTemplateRequest createJobTemplateRequest = CreateJobTemplateRequest.builder()
                .jobTemplateId(model.getJobTemplateId())
                .abortConfig(abortConfig)
                .description(model.getDescription())
                .document(model.getDocument())
                .documentSource(model.getDocumentSource())
                .jobArn(model.getJobArn())
                .jobExecutionsRolloutConfig(rolloutConfig)
                .presignedUrlConfig(presignedUrlConfig)
                .timeoutConfig(timeoutConfig)
                .tags(tags)
                .build();

        try {
            CreateJobTemplateResponse response = proxy.injectCredentialsAndInvokeV2(createJobTemplateRequest, iotClient::createJobTemplate);
            logger.log(String.format("%s [%s] created successfully", ResourceModel.TYPE_NAME, model.getJobTemplateId()));

            return ProgressEvent.defaultSuccessHandler(ResourceModel.builder()
                    .jobTemplateArn(response.jobTemplateArn())
                    .jobTemplateId(response.jobTemplateId())
                    .build());

        } catch (final ResourceAlreadyExistsException e){
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, model.getJobTemplateId());
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
