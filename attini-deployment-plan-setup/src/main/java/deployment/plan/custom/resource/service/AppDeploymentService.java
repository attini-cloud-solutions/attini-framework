package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import deployment.plan.custom.resource.CfnRequestType;
import deployment.plan.custom.resource.CustomResourceHandler;
import deployment.plan.custom.resource.StackType;

public class AppDeploymentService {

    private static final Logger logger = Logger.getLogger(AppDeploymentService.class);


    private final DeployStatesFacade deployStatesFacade;
    private final DeploymentPlanStateFactory deploymentPlanStateFactory;

    public AppDeploymentService(DeployStatesFacade deployStatesFacade, DeploymentPlanStateFactory deploymentPlanStateFactory) {
        this.deployStatesFacade = requireNonNull(deployStatesFacade, "deployStatesFacade");
        this.deploymentPlanStateFactory = requireNonNull(deploymentPlanStateFactory, "deploymentPlanStateFactory");
    }


    public void saveAppDeploymentState(RegisterDeployOriginDataRequest request, String appPipelineName ){

        DeploymentPlanResourceState deploymentPlanResourceState = deploymentPlanStateFactory.create(request,
                                                                                                    StackType.APP);

        if (request.getCfnRequestType() == CfnRequestType.UPDATE){
            deployStatesFacade.deleteDeploymentPlanState(request.getOldSfnArn());
        }
        logger.info(request.getRunners());
        logger.info(request.getRunners().get(0));
        logger.info(request.getRunners().get(0).toString());
        logger.info(request.getRunners().get(0).getCluster());

        deployStatesFacade.saveDeploymentPlanState(deploymentPlanResourceState);
        request.getRunners()
               .forEach(runner -> deployStatesFacade.saveRunnerState(runner, deploymentPlanResourceState));

        deployStatesFacade.saveAppDeploymentData(deploymentPlanResourceState, appPipelineName);

    }
}
