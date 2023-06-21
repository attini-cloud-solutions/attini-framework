package attini.action.actions.posthook;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Map;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.SendUsageDataFacade;
import attini.action.facades.artifactstore.ArtifactStoreFacade;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.domain.DistributionContext;

public class PostPipelineHook {

    private static final Logger logger = Logger.getLogger(PostPipelineHook.class);
    private final ObjectMapper objectMapper;
    private final SendUsageDataFacade sendUsageDataFacade;
    private final DeployOriginFacade deployOriginFacade;
    private final DeploymentPlanDataFacade deploymentPlanDataFacade;
    private final ArtifactStoreFacade artifactStoreFacade;

    public PostPipelineHook(ObjectMapper objectMapper,
                            SendUsageDataFacade sendUsageDataFacade,
                            DeployOriginFacade deployOriginFacade,
                            DeploymentPlanDataFacade deploymentPlanDataFacade,
                            ArtifactStoreFacade artifactStoreFacade) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.sendUsageDataFacade = requireNonNull(sendUsageDataFacade, "sendUsageDataFacade");
        this.deployOriginFacade = requireNonNull(deployOriginFacade, "deployOriginFacade");
        this.deploymentPlanDataFacade = requireNonNull(deploymentPlanDataFacade, "deploymentPlanDataFacade");
        this.artifactStoreFacade = requireNonNull(artifactStoreFacade, "artifactStoreFacade");
    }

    public void handlePostPipelineHook(Map<String, Object> input) {
        logger.info("Running post pipeline hook");
        JsonNode jsonNode = objectMapper.valueToTree(input);

        sendUsageDataFacade.sendEndUsage(jsonNode.get("detail")
                                                 .get("executionArn")
                                                 .asText(),
                                         jsonNode.get("detail")
                                                 .get("status")
                                                 .asText(),
                                         jsonNode.get("detail")
                                                 .get("stateMachineArn")
                                                 .asText(),
                                         Instant.ofEpochSecond(Long.parseLong(jsonNode.get("detail")
                                                                                      .get("startDate")
                                                                                      .asText())));

        String deployOriginSourceName = getDeployOriginSourceName(jsonNode);

        DistributionContext context = deployOriginFacade.getContext(jsonNode.get("detail")
                                                                            .get("executionArn")
                                                                            .asText(),
                                                                    deployOriginSourceName);



        if ("SUCCEEDED".equals(jsonNode.path("detail")
                                       .path("status")
                                       .asText())) {

            saveDistributionOutput(jsonNode.get("detail")
                                           .get("output"), context);
        } else {
            deployOriginFacade.addExecutionError(deployOriginSourceName,
                                                 context.getObjectIdentifier(),
                                                 jsonNode.path("detail")
                                                         .path("cause")
                                                         .asText(),
                                                 jsonNode.path("detail")
                                                         .path("error")
                                                         .asText());
        }
    }

    private String getDeployOriginSourceName(JsonNode input) {
        try {
            JsonNode deploymentPlanInput = objectMapper.readTree(input.get("detail")
                                                                       .get("input")
                                                                       .asText());

            if (deploymentPlanInput.has("distributionName")) {
                String distributionName = deploymentPlanInput.get("distributionName").asText();
                String objectIdentifier = deploymentPlanInput.get("objectIdentifier").asText();
                return "%s-%s".formatted(objectIdentifier.split("/")[0], distributionName);
            }

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return deploymentPlanDataFacade.getDeploymentPlan(input.get("detail")
                                                               .get("stateMachineArn")
                                                               .asText()).getDeployOriginSourceName();


    }

    private void saveDistributionOutput(JsonNode output, DistributionContext context) {
        try {


            artifactStoreFacade.saveDistributionOutput(context.getEnvironment(),
                                                       context.getDistributionName(),
                                                       context.getDistributionId(),
                                                       objectMapper.readTree(output.asText()));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Could not read distribution meta data from output block", e);
        }
    }

}
