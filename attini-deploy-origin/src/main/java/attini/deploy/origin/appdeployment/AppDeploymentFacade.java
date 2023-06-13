package attini.deploy.origin.appdeployment;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import attini.deploy.origin.DistributionData;
import attini.deploy.origin.config.DistributionType;
import attini.deploy.origin.deploystack.DeployDataFacade;
import attini.domain.DistributionContext;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;

public class AppDeploymentFacade {

    private static final Logger logger = Logger.getLogger(AppDeploymentFacade.class);
    private final SfnClient sfnClient;
    private final AppDeploymentDataFacade appDeploymentDataFacade;
    private final ObjectMapper objectMapper;
    private final DeployDataFacade deployDataFacade;

    public AppDeploymentFacade(SfnClient sfnClient,
                               AppDeploymentDataFacade appDeploymentDataFacade,
                               ObjectMapper objectMapper,
                               DeployDataFacade deployDataFacade) {
        this.sfnClient = requireNonNull(sfnClient, "sfnClient");
        this.appDeploymentDataFacade = requireNonNull(appDeploymentDataFacade, "appDeploymentDataFacade");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.deployDataFacade = requireNonNull(deployDataFacade, "deployDataFacade");
    }

    public void runAppDeploymentPlan(AppConfig appConfig,
                                     DistributionContext distributionContext,
                                     DistributionData distributionData, long deployTime) {

        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.set("appConfig", appConfig.getConfig());
        objectNode.put("distributionName", distributionContext.getDistributionName().asString());
        objectNode.put("objectIdentifier", distributionContext.getObjectIdentifier().asString());


        AppDeploymentDataFacade.AppDeploymentData appDeploymentData = appDeploymentDataFacade.getSfnArn(
                distributionContext.getEnvironment(),
                appConfig.getAppDeploymentPlan());

        sfnClient.startExecution(StartExecutionRequest.builder()
                                                      .input(objectNode.toString())
                                                      .stateMachineArn(appDeploymentData.sfnArn())
                                                      .build());

        logger.info("Started app deployment plan");

        deployDataFacade.saveAppDeployment(DeployDataFacade.SaveDeploymentDataRequest.builder()
                                                                                     .distributionData(distributionData)

                                                                                     .distributionContext(
                                                                                             distributionContext)
                                                                                     .deployTime(deployTime)
                                                                                     .isUnchanged(false)
                                                                                     .build(),
                                           appDeploymentData.stackName(),
                                           appDeploymentData.sfnArn());


    }
}
