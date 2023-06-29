/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.getdeployorigindata;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import attini.action.actions.getdeployorigindata.dependencies.DependencyFacade;
import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.deployorigin.DeploymentName;
import attini.action.facades.stackdata.AppDeploymentPlanDataFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.domain.DeployOriginData;

public class GetAppDeployOriginDataHandler {

    private static final Logger logger = Logger.getLogger(GetAppDeployOriginDataHandler.class);
    private final DeployOriginFacade deployOriginFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final ObjectMapper objectMapper;
    private final DeploymentPlanDataFacade deploymentPlanDataFacade;
    private final AppDeploymentPlanDataFacade appDeploymentPlanDataFacade;
    private final DependencyFacade dependencyFacade;


    public GetAppDeployOriginDataHandler(DeployOriginFacade deployOriginFacade,
                                         StepFunctionFacade stepFunctionFacade,
                                         ObjectMapper objectMapper,
                                         DeploymentPlanDataFacade deploymentPlanDataFacade,
                                         AppDeploymentPlanDataFacade appDeploymentPlanDataFacade,
                                         DependencyFacade dependencyFacade) {
        this.deployOriginFacade = requireNonNull(deployOriginFacade, "deployOriginFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.deploymentPlanDataFacade = requireNonNull(deploymentPlanDataFacade, "deploymentPlanDataFacade");
        this.appDeploymentPlanDataFacade = requireNonNull(appDeploymentPlanDataFacade, "appDeploymentPlanDataFacade");
        this.dependencyFacade = requireNonNull(dependencyFacade, "dependenciesFacade");
    }

    public Map<String, Object> getAppDeployOriginData(Map<String, Object> input) {

        logger.info("Getting app deployment data");

        GetAppDeployOriginDataRequest getDeployOriginDataRequest = toGetAppDeployOriginDataRequest(
                input);

        DeploymentPlanStateData deploymentPlan = deploymentPlanDataFacade.getDeploymentPlan(getDeployOriginDataRequest.getSfnArn());


        DeploymentName deploySourceName = DeploymentName.create(deploymentPlan.getEnvironment(),
                                                                getDeployOriginDataRequest.getDistributionName());

        final DeployOriginData deployOriginData = deployOriginFacade.getDeployOriginData(getDeployOriginDataRequest.getObjectIdentifier(),
                                                                                         deploySourceName);

        cancelPreviousExecutions(getDeployOriginDataRequest, deploySourceName);

        deployOriginFacade.setSfnExecutionArn(getDeployOriginDataRequest.getExecutionArn(),
                                              getDeployOriginDataRequest.getObjectIdentifier(),
                                              deploySourceName);



        DeployOriginData platformDeploymentData =
                deployOriginFacade.getDeployOriginData(getDeployOriginDataRequest.getPlatformDistributionIdentifier(),
                                                       DeploymentName.create(deploymentPlan.getEnvironment(),
                                                                             getDeployOriginDataRequest.getPlatformDistributionName()));

        ObjectNode payload = objectMapper.valueToTree(input.get("payload"));

        payload.remove("appConfig");
        payload.remove("distributionId");
        payload.remove("distributionName");
        payload.remove("objectIdentifier");
        JsonNode appDeploymentPlanNode = payload.remove("appDeploymentPlan");
        if (appDeploymentPlanNode == null) {
            throw new IllegalArgumentException("Error when reading appDeploymentPlan from payload.");
        }

        ObjectNode deployOriginDataNode = objectMapper.valueToTree(deployOriginData);
        ObjectNode platformDistributionData = objectMapper.createObjectNode();
        platformDistributionData.put("distributionName", platformDeploymentData.getDistributionName().asString());
        platformDistributionData.put("sourceLocation",
                                     "s3://%s/%s".formatted(platformDeploymentData.getDeploySource()
                                                                                  .getAttiniDeploySourceBucket(),
                                                            platformDeploymentData.getDeploySource()
                                                                                  .getAttiniDeploySourcePrefix()));
        deployOriginDataNode.set("platformDistributionData", platformDistributionData);

        return Map.of("deploymentOriginData",
                      deployOriginDataNode,
                      "output",
                      createOutput(deploymentPlan),
                      "dependencies",
                      dependencyFacade.getDependencies(deployOriginData.getEnvironment(),
                                                       deployOriginData.getDistributionName()),
                      "environment",
                      deployOriginData.getEnvironment().asString(),
                      "customData",
                      payload,
                      "appConfig",
                      input.get("appConfig"),
                      "stackParameters",
                      appDeploymentPlanDataFacade.getStackParameters(appDeploymentPlanNode.asText(),
                                                                     deployOriginData.getEnvironment()));


    }

    private HashMap<String, Object> createOutput(DeploymentPlanStateData deploymentPlan) {

        try {
            JsonNode defaults = objectMapper.readTree(deploymentPlan.getPayloadDefaults());

            if (!defaults.path("output").isMissingNode()) {
                return objectMapper.convertValue(defaults.path("output"), new TypeReference<>() {
                });
            }
            return new HashMap<>();
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void cancelPreviousExecutions(GetAppDeployOriginDataRequest getDeployOriginDataRequest,
                                          DeploymentName deploySourceName) {

        Set<String> currentExecutions = stepFunctionFacade.listExecutions(getDeployOriginDataRequest.getSfnArn())
                                                          .filter(execution -> !execution.equals(
                                                                  getDeployOriginDataRequest.getExecutionArn()))
                                                          .collect(Collectors.toSet());

        if (!currentExecutions.isEmpty()) {
            deployOriginFacade.getLatestExecutionArns(deploySourceName)
                              .stream()
                              .filter(currentExecutions::contains)
                              .forEach(execution -> stepFunctionFacade.stopExecution(execution,
                                                                                     "Stopped due to new execution started"));
        }
    }


    public GetAppDeployOriginDataRequest toGetAppDeployOriginDataRequest(Map<String, Object> input) {
        return objectMapper.convertValue(input, GetAppDeployOriginDataRequest.class);
    }

}
