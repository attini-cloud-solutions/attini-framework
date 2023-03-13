/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.cloudformation;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import attini.step.guard.StackError;
import attini.step.guard.StepFunctionFacade;
import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.StackData;
import attini.step.guard.stackdata.StackDataFacade;


public class CfnEventHandler {
    private static final Logger logger = Logger.getLogger(CfnEventHandler.class);
    private final StepFunctionFacade stepFunctionFacade;
    private final StackDataFacade stackDataFacade;
    private final CfnOutputCreator cfnOutputCreator;
    private final StackErrorResolver stackErrorResolver;
    private final DeployDataFacade deployDataFacade;
    private final CfnSnsEventTypeResolver cfnSnsEventTypeResolver;

    public CfnEventHandler(StepFunctionFacade stepFunctionFacade,
                           StackDataFacade stackDataFacade,
                           CfnOutputCreator cfnOutputCreator,
                           StackErrorResolver stackErrorResolver,
                           DeployDataFacade deployDataFacade,
                           CfnSnsEventTypeResolver cfnSnsEventTypeResolver) {
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "sfnResponseSender");
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.cfnOutputCreator = requireNonNull(cfnOutputCreator, "cfnOutputCreator");
        this.stackErrorResolver = requireNonNull(stackErrorResolver, "stackErrorResolver");
        this.deployDataFacade = requireNonNull(deployDataFacade, "deployDataFacade");
        this.cfnSnsEventTypeResolver = requireNonNull(cfnSnsEventTypeResolver, "cfnSnsEventTypeResolver");
    }

    public void respondToManualCfnEvent(CloudFormationManualTriggerEvent manualTrigger) {
        logger.info("Reacting to manually triggered event = " + manualTrigger);
        try {
            if (CfnStatuses.isUpdated(manualTrigger.getResourceStatus())) {
                logger.info("Stack action manually triggered with success, time to respond");
                String cfnOutput = cfnOutputCreator.createCfnOutput(manualTrigger);

                manualTrigger.getSfnResponseToken()
                             .ifPresent(token -> stepFunctionFacade.sendTaskSuccess(token, cfnOutput));
            } else if (CfnStatuses.isDeleted(manualTrigger.getResourceStatus())) {
                if (manualTrigger.getDesiredState() == DesiredState.DELETED) {
                    String cfnOutput = cfnOutputCreator.createCfnOutput(manualTrigger);
                    manualTrigger.getSfnResponseToken()
                                 .ifPresent(token -> stepFunctionFacade.sendTaskSuccess(token, cfnOutput));
                }
                logger.info("stack deleted, cleaning up resources");
                stackDataFacade.deleteCfnStack(manualTrigger);
            } else {
                logger.info("Stack action manually triggered with failed status, time to respond");
                StackError error = stackErrorResolver.resolveError(manualTrigger);
                deployDataFacade.addExecutionError(manualTrigger.getAttiniContext(),
                                                   "init-stack failed with error: " + error.getMessage());
                manualTrigger.getSfnResponseToken()
                             .ifPresent(token -> stepFunctionFacade.sendTaskFailure(token,
                                                                                    error.getMessage(),
                                                                                    error.getErrorStatus()));

            }
        } catch (Exception e) {
            logger.error("There was an error when creating cfnOutput", e);
            manualTrigger.getSfnResponseToken()
                         .ifPresent(token -> stepFunctionFacade.sendTaskFailure(token, e.getMessage(), "unknown"));
        }


    }

    public void respondToCloudFormationSnsEvent(CloudFormationSnsEventImpl event) {


        switch (cfnSnsEventTypeResolver.resolve(event)) {
            case RESOURCE_FAILED -> addResourceError(event);
            case STACK_FAILED -> {
                addResourceError(event);
                handleCfnStackFailed(event, stackDataFacade.getStackData(event));
            }
            case STACK_UPDATED -> sendStepCompleted(event);
            case STACK_DELETED -> deleteStack(event);
            case RESOURCE_UPDATE, STACK_IN_PROGRESS -> logger.info("Not responding to status=" + event.getResourceStatus());
        }
    }

    private void deleteStack(CloudFormationSnsEventImpl event) {
        StackData stackData = stackDataFacade.getStackData(event);
        logger.info("Desired state of stack is = " + stackData.getDesiredState());
        if (stackData.getDesiredState() == DesiredState.DELETED) {
            stackData.getSfnToken()
                     .ifPresent(token -> stepFunctionFacade.sendTaskSuccess(token,
                                                                            cfnOutputCreator.createCfnOutput(
                                                                                    stackData,
                                                                                    event)));
        } else {
            handleCfnStackFailed(event, stackData);
        }
        logger.info("stack deleted, cleaning up resources");
        stackDataFacade.deleteCfnStack(event);
    }

    private void sendStepCompleted(CloudFormationSnsEventImpl event) {
        StackData stackData = stackDataFacade.getStackData(event);
        logger.info("Stack action successful, time to respond");
        try {
            String cfnOutput = cfnOutputCreator.createCfnOutput(stackData, event);
            stackData.getSfnToken().ifPresent(token -> stepFunctionFacade.sendTaskSuccess(token, cfnOutput));
        } catch (Exception e) {
            logger.error("There was an error when creating cfnOutput", e);
            stackData.getSfnToken()
                     .ifPresent(token -> stepFunctionFacade.sendTaskFailure(token,
                                                                            e.getMessage(),
                                                                            "unknown"));
        }
    }

    private void addResourceError(CloudFormationSnsEventImpl event) {
        event.getResourceStatusReason().ifPresent(error -> {
            stackDataFacade.saveError(event, error);
            StackData stackData = stackDataFacade.getStackData(event);
            deployDataFacade.addStackError(event, stackData, error);
        });
    }

    private void handleCfnStackFailed(CloudFormationSnsEventImpl event, StackData stackData) {
        logger.info("Stack action failed, time to respond");
        StackError error = stackErrorResolver.resolveError(event);
        stackData.getSfnToken()
                 .ifPresent(token -> stepFunctionFacade.sendTaskFailure(token,
                                                                        error.getMessage(),
                                                                        error.getErrorStatus()));
        deployDataFacade.addExecutionError(stackData,
                                           "Step: " + stackData.getStepName() + " failed with error: " + error.getMessage());
    }
}
