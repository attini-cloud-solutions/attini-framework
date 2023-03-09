package attini.action.actions.deploycloudformation;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.facades.stackdata.StackDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.facades.stepguard.StepGuardFacade;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

public class CfnErrorHandler {

    private static final Logger logger = Logger.getLogger(CfnErrorHandler.class);

    private final CfnStackFacade cfnStackFacade;
    private final StepGuardFacade stepGuardFacade;
    private final StackDataFacade stackDataFacade;
    private final StepFunctionFacade stepFunctionFacade;
    public CfnErrorHandler(CfnStackFacade cfnStackFacade,
                           StepGuardFacade stepGuardFacade,
                           StackDataFacade stackDataFacade,
                           StepFunctionFacade stepFunctionFacade) {
        this.cfnStackFacade = requireNonNull(cfnStackFacade, "cfnStackFacade");
        this.stepGuardFacade = requireNonNull(stepGuardFacade, "stepGuardFacade");
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
    }

    protected void handleNoUpdatesToPerformedState(StackData stackData, String resourceStatus) {
        String stackName = stackData.getStackConfiguration().getStackName();
        logger.info(String.format("No updates are to be performed to stack %s", stackName));
        stackDataFacade.saveStackData(stackData);
        stepGuardFacade.notifyStepCompleted(stackData, resourceStatus);



    }

    protected void handleValidationError(StackData stackData, CloudFormationException e) {
        String errorMessage = validationErrorMessage(e.awsErrorDetails(),stackData.getStackConfiguration());
        logger.error(errorMessage, e);
        sendDeploymentPlanExecutionError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                         errorMessage,
                                         "ValidationError");
        throw new DeployCfnException(errorMessage, e);
    }
    private static String validationErrorMessage(AwsErrorDetails errorDetails, StackConfiguration stackConfiguration){
        if (errorDetails.errorMessage().contains("S3 error: Access Denied")){
            return "Could not get the template file from S3, " +
                   "this is most likely due to that that the specified template does not exist in S3 or that attini " +
                   "does not have access to S3, specified template = "+stackConfiguration.getTemplate();
        }

        return "A validation error occurred, message = " +errorDetails.errorMessage();
    }

    protected void handleRollbackCompleteState(StackData stackData) {
        cfnStackFacade.deleteStack(stackData);
        sendDeploymentPlanExecutionError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                         "Stack is in ROLLBACK_COMPLETE state, will delete stack",
                                         "RollBackCompleteState");
    }

    protected void sendDeploymentPlanExecutionError(String token,
                                                  String errorMessage,
                                                  String error) {
        stepFunctionFacade.sendError(token, errorMessage, error);
    }
}
