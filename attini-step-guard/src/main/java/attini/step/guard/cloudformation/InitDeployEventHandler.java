package attini.step.guard.cloudformation;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import attini.step.guard.StepFunctionFacade;
import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.stackdata.InitDeployData;
import attini.step.guard.stackdata.StackDataFacade;

public class InitDeployEventHandler {

    private static final Logger logger = Logger.getLogger(InitDeployEventHandler.class);
    private final StackDataFacade stackDataFacade;
    private final DeployDataFacade deployDataFacade;

    private final StepFunctionFacade stepFunctionFacade;
    private final StackErrorResolver stackErrorResolver;
    private final CfnSnsEventTypeResolver cfnSnsEventTypeResolver;


    public InitDeployEventHandler(StackDataFacade stackDataFacade,
                                  DeployDataFacade deployDataFacade,
                                  StepFunctionFacade stepFunctionFacade,
                                  StackErrorResolver stackErrorResolver,
                                  CfnSnsEventTypeResolver cfnSnsEventTypeResolver) {
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.deployDataFacade = requireNonNull(deployDataFacade, "deployDataFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.stackErrorResolver = requireNonNull(stackErrorResolver, "stackErrorResolver");
        this.cfnSnsEventTypeResolver = requireNonNull(cfnSnsEventTypeResolver, "cfnSnsEventTypeResolver");
    }

    public void respondToManualInitDeployEvent(InitDeployManualTriggerEvent event) {
        logger.info("Reacting to init deploy manual approval event");
        InitDeployData initDeployData = stackDataFacade.getInitDeployData(event.stackName());
        deployDataFacade.addDeploymentPlanData(initDeployData);
        stepFunctionFacade.runDeploymentPlan(initDeployData.getSfnArns());
    }

    public void respondToInitDeployCfnEvent(InitDeploySnsEvent initDeploySnsEvent) {
        String resourceStatus = initDeploySnsEvent.getResourceStatus();

        switch (cfnSnsEventTypeResolver.resolve(initDeploySnsEvent)) {
            case STACK_UPDATED -> {
                logger.info("Init stack successful, time to respond");
                InitDeployData initDeployData = stackDataFacade.getInitDeployData(initDeploySnsEvent.getStackName(),
                                                                                  initDeploySnsEvent.getClientRequestToken());
                deployDataFacade.addDeploymentPlanData(initDeployData);
                stepFunctionFacade.runDeploymentPlan(initDeployData.getSfnArns());
            }
            case RESOURCE_FAILED -> addResourceError(initDeploySnsEvent);
            case STACK_FAILED -> {
                logger.info("Init stack failed, time to respond");
                addResourceError(initDeploySnsEvent);
                stackDataFacade.removeInitTemplateMd5(initDeploySnsEvent.getStackName());
                InitDeployData initDeployData = stackDataFacade.getInitDeployData(initDeploySnsEvent.getStackName(),
                                                                                  initDeploySnsEvent.getClientRequestToken());
                deployDataFacade.addExecutionError(initDeployData,
                                                   "init-stack failed with error: %s".formatted(stackErrorResolver.resolveError(
                                                           initDeploySnsEvent).getMessage()));
            }
            case STACK_DELETED -> {
                if (isNotRecreateCall(initDeploySnsEvent)) {
                    logger.info("Init stack deleted. Cleaning up related resources.");
                    stackDataFacade.deleteInitDeploy(initDeploySnsEvent);
                } else {
                    logger.info("Stack is being recreated, leaving related resources.");
                }
            }
            case RESOURCE_UPDATE, STACK_IN_PROGRESS -> logger.debug("Not responding to status=" + resourceStatus);
        }


    }

    private void addResourceError(InitDeploySnsEvent initDeploySnsEvent) {
        initDeploySnsEvent.getResourceStatusReason()
                          .ifPresent(error -> {
                              stackDataFacade.saveInitDeployError(initDeploySnsEvent, error);
                              InitDeployData initDeployData = stackDataFacade.getInitDeployData(initDeploySnsEvent.getStackName(),
                                                                                                initDeploySnsEvent.getClientRequestToken());
                              deployDataFacade.addInitStackError(initDeploySnsEvent, initDeployData, error);
                          });
    }

    private static boolean isNotRecreateCall(CloudFormationEvent event) {
        return event.getClientRequestToken() != null && !event.getClientRequestToken().startsWith("recreate-call");
    }

}
