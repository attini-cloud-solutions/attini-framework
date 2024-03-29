package attini.action.actions.deploycloudformation;

import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.resolveError;
import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.jboss.logging.Logger;

import attini.action.domain.DesiredState;
import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.facades.stepguard.StepGuardFacade;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

public class DeployCfnCrossRegionService {

    private static final Logger logger = Logger.getLogger(DeployCfnCrossRegionService.class);

    private final CfnStackFacade cfnStackFacade;
    private final StepGuardFacade stepGuardFacade;
    private final ResourceStateFacade resourceStateFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final CfnErrorHandler cfnErrorHandler;


    public DeployCfnCrossRegionService(CfnStackFacade cfnStackFacade,
                                       StepGuardFacade stepGuardFacade,
                                       ResourceStateFacade resourceStateFacade,
                                       StepFunctionFacade stepFunctionFacade,
                                       CfnErrorHandler cfnErrorHandler) {
        this.cfnStackFacade = requireNonNull(cfnStackFacade, "cfnStackFacade");
        this.stepGuardFacade = requireNonNull(stepGuardFacade, "stepGuardFacade");
        this.resourceStateFacade = requireNonNull(resourceStateFacade, "stackDataFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.cfnErrorHandler = requireNonNull(cfnErrorHandler, "cfnErrorHandler");
    }


    public void deployWithPolling(StackData stackData) {
        try {

            StackStatus stackStatus = cfnStackFacade.getStackStatus(stackData);
            String clientRequestToken = stackStatus.clientRequestToken();

            if (isSameExecution(stackData, clientRequestToken) && stackIsBeingUpdated(stackStatus)) {
                logger.info("stack is being updated, will retry");
                stepFunctionFacade.sendError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                             "Is in progress",
                                             "IsExecuting");
                return;
            }
            if (isSameExecution(stackData, clientRequestToken)) {
                logger.info("stack is done updating. Will notify step guard of completion");
                stepGuardFacade.notifyStepCompleted(stackData, stackStatus.stackStatus(), stackStatus.stackId());
                return;
            }

            if (isNewExecution(stackData) && shouldDeleteStack(stackData)) {
                logger.info("New request for stack deletion received. Will delete stack");
                cfnStackFacade.deleteStack(stackData);
                resourceStateFacade.saveStackData(stackData);
                stepFunctionFacade.sendError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                             "Is in progress",
                                             "IsExecuting");
                return;
            }


            if (isNewExecution(stackData)) {
                logger.info("New request for stack update received. Will update stack");
                resourceStateFacade.saveStackData(stackData);
                String stackId = cfnStackFacade.updateStackCrossRegion(stackData);
                resourceStateFacade.saveStackId(stackId,
                                              stackData.getStackConfiguration());
                stepFunctionFacade.sendError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                             "Is in progress",
                                             "IsExecuting");
                return;
            }

            logger.error("The stack is being updated again by another account/region. Will abort.");

            throw new IllegalStateException("The stack is being updated again by another account/region");

        } catch (CloudFormationException e) {
            logger.info("Original exception message: "+ e.getMessage());
            CloudFormationErrorResolver.CloudFormationError cloudFormationError = resolveError(e);
            switch (cloudFormationError) {
                case NO_STACK_EXISTS -> {
                    if (!shouldDeleteStack(stackData)) {
                        logger.info("No stack found. Will create the stack");
                        String stackId = cfnStackFacade.createStackCrossRegion(stackData);
                        resourceStateFacade.saveStackData(stackData, stackId);
                        stepFunctionFacade.sendError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                                     "Is in progress",
                                                     "IsExecuting");
                    } else {
                        logger.info("No stack found so no delete required");
                        cfnErrorHandler.handleNoUpdatesToPerformedState(stackData, "DELETE_COMPLETE");
                    }

                }
                case ROLLBACK_COMPLETE -> cfnErrorHandler.handleRollbackCompleteState(stackData);
                case VALIDATION_ERROR -> cfnErrorHandler.handleValidationError(stackData, e);
                case NO_UPDATE_TO_PERFORM ->
                        cfnErrorHandler.handleNoUpdatesToPerformedState(stackData, "UPDATE_COMPLETE");
                case ACCESS_DENIED -> throw new DeployCfnException(
                        "Could not update stack, access denied. Original error message: " + e.getMessage()  ,
                        e);
                default -> {
                    logger.error(e.awsErrorDetails().errorCode());
                    logger.error(e.awsErrorDetails().errorMessage());
                    throw new DeployCfnException("An error occurred when deploying the CloudFormation stack, error: " + e.awsErrorDetails()
                                                                                                          .errorMessage(),
                                                 e);
                }
            }
        }
    }

    private static boolean shouldDeleteStack(StackData stackData) {
        return stackData.getStackConfiguration().getDesiredState() == DesiredState.DELETED;
    }

    private static boolean stackIsBeingUpdated(StackStatus stackStatus) {
        return stackStatus.stackState() == StackStatus.StackState.UPDATE_IN_PROGRESS;
    }

    private static boolean isSameExecution(StackData stackData, String clientRequestToken) {
        if(clientRequestToken == null){
            return false;
        }
        return clientRequestToken.startsWith(stackData.getDeploymentPlanExecutionMetadata()
                                                      .executionArn()
                                                      .extractExecutionId());
    }

    private boolean isNewExecution(StackData stackData) {
        Optional<SfnExecutionArn> stacksExecutionId = resourceStateFacade.getStacksSfnExecutionArn(stackData.getStackConfiguration());
        return stacksExecutionId.filter(sfnExecutionArn -> stackData.getDeploymentPlanExecutionMetadata()
                                                                    .executionArn()
                                                                    .equals(sfnExecutionArn))
                                .isEmpty();
    }

}
