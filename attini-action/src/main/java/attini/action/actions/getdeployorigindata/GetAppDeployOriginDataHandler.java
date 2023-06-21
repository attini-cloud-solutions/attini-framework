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

import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.AppDeploymentPlanDataFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stackdata.DistributionDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.domain.DeployOriginData;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class GetAppDeployOriginDataHandler {

    private static final Logger logger = Logger.getLogger(GetAppDeployOriginDataHandler.class);
    private final DeployOriginFacade deployOriginFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final DistributionDataFacade distributionDataFacade;
    private final ObjectMapper objectMapper;
    private final DeploymentPlanDataFacade deploymentPlanDataFacade;
    private final AppDeploymentPlanDataFacade appDeploymentPlanDataFacade;


    public GetAppDeployOriginDataHandler(DeployOriginFacade deployOriginFacade,
                                         StepFunctionFacade stepFunctionFacade,
                                         DistributionDataFacade distributionDataFacade,
                                         ObjectMapper objectMapper,
                                         DeploymentPlanDataFacade deploymentPlanDataFacade,
                                         AppDeploymentPlanDataFacade appDeploymentPlanDataFacade) {
        this.deployOriginFacade = requireNonNull(deployOriginFacade, "deployOriginFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.distributionDataFacade = requireNonNull(distributionDataFacade, "distributionDataFacade");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.deploymentPlanDataFacade = requireNonNull(deploymentPlanDataFacade, "deploymentPlanDataFacade");
        this.appDeploymentPlanDataFacade = requireNonNull(appDeploymentPlanDataFacade, "appDeploymentPlanDataFacade");
    }

    public Map<String, Object> getAppDeployOriginData(Map<String, Object> input) {

        logger.info("Getting app deployment data");

        GetAppDeployOriginDataRequest getDeployOriginDataRequest = toGetAppDeployOriginDataRequest(
                input);

        DeploymentPlanStateData deploymentPlan = deploymentPlanDataFacade.getDeploymentPlan(getDeployOriginDataRequest.getSfnArn());


        String deploySourceName = getDeploySourceName(getDeployOriginDataRequest.getDistributionName(),
                                                      deploymentPlan.getEnvironment());
        final DeployOriginData deployOriginData = deployOriginFacade.getDeployOriginData(getDeployOriginDataRequest.getObjectIdentifier(),
                                                                                         deploySourceName);

        cancelPreviousExecutions(getDeployOriginDataRequest, deploySourceName);

        deployOriginFacade.setSfnExecutionArn(getDeployOriginDataRequest.getExecutionArn(),
                                              getDeployOriginDataRequest.getObjectIdentifier(),
                                              deploySourceName);

        Environment environment = deployOriginData.getEnvironment();
        Map<String, Map<String, String>> dependencies = distributionDataFacade.getDistribution(
                                                                                      deployOriginData.getDistributionName(),
                                                                                      environment)
                                                                              .getDependencies()
                                                                              .stream()
                                                                              .map(dependency -> distributionDataFacade.getDistribution(
                                                                                      DistributionName.of(dependency.name()),
                                                                                      environment))
                                                                              .collect(Collectors.toMap(distribution -> distribution.getDistributionName()
                                                                                                                                    .asString(),
                                                                                                        distribution -> {
                                                                                                            HashMap<String, String> map = new HashMap<>();
                                                                                                            map.put("deploymentSourcePrefix",
                                                                                                                    distribution.getDeploymentSourcePrefix());
                                                                                                            distribution.getOutputUrl()
                                                                                                                        .ifPresent(
                                                                                                                                s -> map.put(
                                                                                                                                        "outputUrl",
                                                                                                                                        s));
                                                                                                            return map;
                                                                                                        }));

        ObjectNode payload = objectMapper.valueToTree(input.get("payload"));

        DeployOriginData platformDeploymentData =
                deployOriginFacade.getDeployOriginData(ObjectIdentifier.of(payload.get("platformDistributionIdentifier")
                                                                                  .asText()),
                                                       getDeploySourceName(DistributionName.of(payload.get(
                                                                                                              "platformDistributionName")
                                                                                                      .textValue()),
                                                                           deploymentPlan.getEnvironment()));


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
                      dependencies,
                      "environment",
                      environment.asString(),
                      "customData",
                      payload,
                      "appConfig",
                      input.get("appConfig"),
                      "stackParameters",
                      appDeploymentPlanDataFacade.getStackParameters(appDeploymentPlanNode.asText(), environment));


    }

    private static String getDeploySourceName(DistributionName distributionName,
                                              Environment environment) {
        return "%s-%s".formatted(
                environment
                        .asString(),
                distributionName
                        .asString());
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
                                          String deploySourceName) {

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
