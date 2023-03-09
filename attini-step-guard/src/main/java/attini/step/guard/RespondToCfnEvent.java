/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.InitDeployData;
import attini.step.guard.stackdata.StackData;
import attini.step.guard.stackdata.StackDataFacade;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;


public class RespondToCfnEvent {
    private static final Logger logger = Logger.getLogger(RespondToCfnEvent.class);

    private static final Set<String> FAILED_RESPONSE_CFN_STATUS = Set.of(
            "CREATE_FAILED",
            "UPDATE_FAILED",
            "ROLLBACK_FAILED",
            "ROLLBACK_COMPLETE",
            "DELETE_FAILED",
            "UPDATE_ROLLBACK_FAILED",
            "UPDATE_ROLLBACK_COMPLETE"
    );
    private static final Set<String> SUCCESSFUL_RESPONSE_CFN_STATUS = Set.of(
            "UPDATE_COMPLETE",
            "CREATE_COMPLETE"
    );
    private final SfnResponseSender sfnResponseSender;
    private final SfnClient sfnClient;
    private final StackDataFacade stackDataFacade;
    private final CfnOutputCreator cfnOutputCreator;
    private final StackErrorResolver stackErrorResolver;
    private final PublishEventService publishEventService;
    private final DeployDataFacade deployDataFacade;

    public RespondToCfnEvent(SfnResponseSender sfnResponseSender,
                             SfnClient sfnClient,
                             StackDataFacade stackDataFacade,
                             CfnOutputCreator cfnOutputCreator,
                             StackErrorResolver stackErrorResolver,
                             PublishEventService publishEventService,
                             DeployDataFacade deployDataFacade) {
        this.sfnResponseSender = requireNonNull(sfnResponseSender, "sfnResponseSender");
        this.sfnClient = requireNonNull(sfnClient, "sfnClient");
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.cfnOutputCreator = requireNonNull(cfnOutputCreator, "cfnOutputCreator");
        this.stackErrorResolver = requireNonNull(stackErrorResolver, "stackErrorResolver");
        this.publishEventService = requireNonNull(publishEventService, "publishEventService");
        this.deployDataFacade = requireNonNull(deployDataFacade, "deployDataFacade");
    }

    public void respondToManualCfnEvent(CloudFormationManualTriggerEvent manualTrigger) {
        logger.info("Reacting to manually triggered event = " + manualTrigger);
        try {
            if (SUCCESSFUL_RESPONSE_CFN_STATUS.contains(manualTrigger.getResourceStatus())) {
                logger.info("Stack action manually triggered with success, time to respond");
                String cfnOutput = cfnOutputCreator.createCfnOutput(manualTrigger);

                manualTrigger.getSfnResponseToken()
                              .ifPresent(token -> sfnResponseSender.sendTaskSuccess(token, cfnOutput));
                publishEventService.postStepCompleted(manualTrigger);
            } else if ("DELETE_COMPLETE".equals(manualTrigger.getResourceStatus())) {
                if (manualTrigger.getDesiredState() == DesiredState.DELETED) {
                    String cfnOutput = cfnOutputCreator.createCfnOutput(manualTrigger);
                    manualTrigger.getSfnResponseToken()
                                  .ifPresent(token -> sfnResponseSender.sendTaskSuccess(token, cfnOutput));
                }
                logger.info("stack deleted, cleaning up resources");
                stackDataFacade.deleteCfnStack(manualTrigger);
                publishEventService.postStepCompleted(manualTrigger);
            } else {
                logger.info("Stack action manually triggered with failed status, time to respond");
                StackError error = stackErrorResolver.resolveError(manualTrigger);
                deployDataFacade.addExecutionError(manualTrigger.getAttiniContext(), "init-stack failed with error: " + error.getMessage());
                manualTrigger.getSfnResponseToken()
                              .ifPresent(token -> sfnResponseSender.sendTaskFailure(token,
                                                                                    error.getMessage(),
                                                                                    error.getErrorStatus()));
                publishEventService.postStepCompleted(manualTrigger, error);

            }
        } catch (Exception e) {
            logger.error("There was an error when creating cfnOutput", e);
            manualTrigger.getSfnResponseToken()
                          .ifPresent(token -> sfnResponseSender.sendTaskFailure(token, e.getMessage(), "unknown"));
        }


    }


    public void respondToManualInitDeployEvent(InitDeployManualTriggerEvent event){
        InitDeployData initDeployData = stackDataFacade.getInitDeployData(event.getStackName());
        deployDataFacade.addDeploymentPlanData(initDeployData);
        runDeploymentPlan(initDeployData.getSfnArns());
    }

    public void respondToInitDeployCfnEvent(InitDeploySnsEvent initDeploySnsEvent) {

        if (FAILED_RESPONSE_CFN_STATUS.contains(initDeploySnsEvent.getResourceStatus())){
            initDeploySnsEvent.getResourceStatusReason().ifPresent(error -> {
                stackDataFacade.saveInitDeployError(initDeploySnsEvent, error);
                //TODO we need to make the error handling more uniform.
                InitDeployData initDeployData = stackDataFacade.getInitDeployData(initDeploySnsEvent.getStackName(), initDeploySnsEvent.getClientRequestToken());
                deployDataFacade.addInitStackError(initDeploySnsEvent, initDeployData, error);
            });
        }

        if (!initDeploySnsEvent.getResourceType().contains("AWS::CloudFormation::Stack")) {
            logger.info("Not a Cloudformation event");
            return;
        }

        if (initDeploySnsEvent.getStackName().equals(initDeploySnsEvent.getLogicalResourceId())) {
            String resourceStatus = initDeploySnsEvent.getResourceStatus();
            logger.info("Time to respond to init event");
            if (SUCCESSFUL_RESPONSE_CFN_STATUS.contains(resourceStatus)) {
                logger.info("Init deployment plan successful, time to respond");
                InitDeployData initDeployData = stackDataFacade.getInitDeployData(initDeploySnsEvent.getStackName(), initDeploySnsEvent.getClientRequestToken());
                deployDataFacade.addDeploymentPlanData(initDeployData);
                runDeploymentPlan(initDeployData.getSfnArns());
            } else if (FAILED_RESPONSE_CFN_STATUS.contains(resourceStatus)) {
                logger.info("Init deployment plan failed, time to respond");
                StackError error = stackErrorResolver.resolveError(initDeploySnsEvent);
                stackDataFacade.removeInitTemplateMd5(initDeploySnsEvent.getStackName());
                InitDeployData initDeployData = stackDataFacade.getInitDeployData(initDeploySnsEvent.getStackName(), initDeploySnsEvent.getClientRequestToken());
                deployDataFacade.addExecutionError(initDeployData, "init-stack failed with error: " + error.getMessage());
            } else if ("DELETE_COMPLETE".equals(resourceStatus) && (initDeploySnsEvent.getClientRequestToken() != null && !initDeploySnsEvent.getClientRequestToken().startsWith("recreate-call"))) {
                logger.info("Cleaning up init stack");
                stackDataFacade.deleteInitDeploy(initDeploySnsEvent);
            } else {
                logger.info("Not responding to status=" + resourceStatus);
            }
        }
    }

    public void respondToCloudFormationSnsEvent(CloudFormationSnsEvent event) {

        if (FAILED_RESPONSE_CFN_STATUS.contains(event.getResourceStatus())){
            event.getResourceStatusReason().ifPresent(error -> {
                stackDataFacade.saveError(event, error);
                StackData stackData = stackDataFacade.getStackData(event);
                deployDataFacade.addStackError(event, stackData, error);
            });
        }


        if (!event.getResourceType().contains("AWS::CloudFormation::Stack")) {
            logger.info("Not a Cloudformation event");
            return;
        }

        if (!event.getStackName().equals(event.getLogicalResourceId())) {
            logger.info("ResourceId and stack name is not the same, doing nothing");
            return;
        }
        try {
            String resourceStatus = event.getResourceStatus();
            if (FAILED_RESPONSE_CFN_STATUS.contains(resourceStatus)) {
                StackData stackData = stackDataFacade.getStackData(event);
                if (isDifferentStackId(event, stackData)) {
                    logger.info(
                            "Stack id in input differs from stackId in dynamo, " +
                            "not responding to step function for stack = " + event.getStackName());
                    return;
                }
                handleCfnStackFailed(event, stackData);
            } else if (SUCCESSFUL_RESPONSE_CFN_STATUS.contains(resourceStatus)) {
                StackData stackData = stackDataFacade.getStackData(event);
                if (isDifferentStackId(event, stackData)) {
                    logger.info(
                            "Stack id in input differs from stackId in dynamo, not responding to step function for stack = " + event.getStackName());
                    return;
                }
                logger.info("Stack action successful, time to respond");
                try {
                    String cfnOutput = cfnOutputCreator.createCfnOutput(stackData, event);
                    stackData.getSfnToken().ifPresent(token -> sfnResponseSender.sendTaskSuccess(token, cfnOutput));
                    publishEventService.postStepCompleted(stackData, event.getStackName());
                } catch (Exception e) {
                    logger.error("There was an error when creating cfnOutput", e);
                    stackData.getSfnToken()
                             .ifPresent(token -> sfnResponseSender.sendTaskFailure(token,
                                                                                   e.getMessage(),
                                                                                   "unknown"));
                }
            } else if ("DELETE_COMPLETE".equals(resourceStatus)) {
                StackData stackData = stackDataFacade.getStackData(event);
                logger.info("Desired state of stack is = " + stackData.getDesiredState());
                if (stackData.getDesiredState() == DesiredState.DELETED) {
                    stackData.getSfnToken()
                             .ifPresent(token -> sfnResponseSender.sendTaskSuccess(token,
                                                                                   cfnOutputCreator.createCfnOutput(
                                                                                           stackData,
                                                                                           event)));
                    publishEventService.postStepCompleted(stackData, event.getStackName());
                }else{
                    if (isDifferentStackId(event, stackData)) {
                        logger.info(
                                "Stack id in input differs from stackId in dynamo, " +
                                "not responding to step function for stack = " + event.getStackName());
                        return;
                    }
                    handleCfnStackFailed(event, stackData);

                }
                logger.info("stack deleted, cleaning up resources");
                stackDataFacade.deleteCfnStack(event);
            } else {
                logger.info("Not responding to status=" + resourceStatus);
            }

        } catch (CloudFormationStackDataNotFoundException e) {
            if (FAILED_RESPONSE_CFN_STATUS.contains(event.getResourceStatus())) {
                logger.warn(
                        "No cloudformation resource found in dynamo, most likely due to " +
                        "the stack failing before the custom resource was triggered, will log error anyways");
            }
        }
    }

    private void handleCfnStackFailed(CloudFormationSnsEvent event, StackData stackData) {
        logger.info("Stack action failed, time to respond");
        StackError error = stackErrorResolver.resolveError(event);
        stackData.getSfnToken()
                 .ifPresent(token -> sfnResponseSender.sendTaskFailure(token,
                                                                       error.getMessage(),
                                                                       error.getErrorStatus()));
        deployDataFacade.addExecutionError(stackData, "Step: " + stackData.getStepName() + " failed with error: " + error.getMessage());
        publishEventService.postStepCompleted(stackData, event.getStackName(), error);
    }


    private boolean isDifferentStackId(CloudFormationEvent cloudFormationEvent, StackData stackData) {
        return cloudFormationEvent.getStackId().isPresent()
               && stackData.getStackId().isPresent()
               && !stackData.getStackId()
                            .get()
                            .equals(cloudFormationEvent.getStackId()
                                                       .get());
    }


    private void runDeploymentPlan(List<String> sfnArns) {
        logger.info("Running " + sfnArns.size() + " deployment plans");
        sfnArns.forEach(arn -> sfnClient.startExecution(StartExecutionRequest.builder()
                                                                             .stateMachineArn(arn)
                                                                             .build())

        );
    }

}
