/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation;

import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.resolveError;
import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError;
import attini.action.domain.DesiredState;
import attini.action.facades.stackdata.StackDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

public class DeployCfnService {
    private static final Logger logger = Logger.getLogger(DeployCfnService.class);
    private final CfnStackFacade cfnStackFacade;
    private final StackDataFacade stackDataFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final CfnErrorHandler cfnErrorHandler;


    public DeployCfnService(CfnStackFacade cfnStackFacade,
                            StackDataFacade stackDataFacade,
                            StepFunctionFacade stepFunctionFacade,
                            CfnErrorHandler cfnErrorHandler) {
        this.cfnStackFacade = requireNonNull(cfnStackFacade, "cfnStackFacade");
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.cfnErrorHandler = requireNonNull(cfnErrorHandler, "cfnErrorHandler");
    }


    public void deployStack(StackData stackData) {

        logger.info(String.format("Trying to update CFN stack %s",
                                  stackData.getStackConfiguration().getStackName()));

        if (stackData.getStackConfiguration().getDesiredState() == DesiredState.DELETED) {
            deleteStack(stackData);
        } else {
            deployWithCallback(stackData);
        }
    }

    private void deleteStack(StackData stackData) {
        try {
            cfnStackFacade.deleteStack(stackData);
            logger.info("Stack " + stackData.getStackConfiguration()
                                            .getStackName() + " is being deleted using callback strategy");
            stackDataFacade.saveStackData(stackData);
            stackDataFacade.saveToken(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                      stackData.getStackConfiguration());


        } catch (CloudFormationException e) {
            CloudFormationError cloudFormationError = resolveError(e);
            if (cloudFormationError.equals(CloudFormationError.VALIDATION_ERROR)) {
                cfnErrorHandler.handleValidationError(stackData, e);
            } else {
                logger.error(e.awsErrorDetails().errorCode());
                logger.error(e.awsErrorDetails().errorMessage());
                throw new DeployCfnException("An error occurred when deleting cfn stack, error: " + e.awsErrorDetails()
                                                                                                     .errorMessage(),
                                             e);
            }

        }

    }

    private void deployWithCallback(StackData stackData) {
        try {
            String stackId = cfnStackFacade.updateCfnStack(stackData);
            stackDataFacade.saveStackData(stackData, stackId);

            stackDataFacade.saveToken(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                      stackData.getStackConfiguration());

        } catch (CloudFormationException e) {
            CloudFormationError cloudFormationError = resolveError(e);
            String stackName = stackData.getStackConfiguration().getStackName();
            switch (cloudFormationError) {
                case NO_UPDATE_TO_PERFORM ->
                        cfnErrorHandler.handleNoUpdatesToPerformedState(stackData, "UPDATE_COMPLETE");
                case ROLLBACK_COMPLETE -> cfnErrorHandler.handleRollbackCompleteState(stackData);
                case NO_STACK_EXISTS -> {
                    logger.info(String.format("could not update stack %s, trying to create it",
                                              stackName));
                    createStack(stackData);
                    logger.info(String.format("Created CFN stack %s", stackName));
                }
                case VALIDATION_ERROR -> cfnErrorHandler.handleValidationError(stackData, e);
                default -> {
                    logger.error(e.awsErrorDetails().errorCode());
                    logger.error(e.awsErrorDetails().errorMessage());
                    throw new DeployCfnException("An error occurred when deploying cfn stack, error: " + e.awsErrorDetails()
                                                                                                          .errorMessage(),
                                                 e);
                }
            }
        }
    }

    private void createStack(StackData stackData) {

        try {
            String stackId = cfnStackFacade.createCfnStack(stackData);
            stackDataFacade.saveStackData(stackData, stackId);

            stackDataFacade.saveToken(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                      stackData.getStackConfiguration());

        } catch (AlreadyExistsException e) {
            stepFunctionFacade.sendError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                         "Attini can not update stack with name=" + stackData.getStackConfiguration()
                                                                                             .getStackName() + ". without the tag \"AttiniResourceType: cloudformation-stack\"",
                                         "AccessDenied");

        } catch (CloudFormationException e) {
            logger.info("Failed to create stack with name = " + stackData.getStackConfiguration().getStackName(), e);
            cfnErrorHandler.sendDeploymentPlanExecutionError(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                             e.getMessage(),
                                             e.awsErrorDetails().errorCode());
        }

    }


}
