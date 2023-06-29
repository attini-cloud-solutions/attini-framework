/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.getdeployorigindata;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.SendUsageDataFacade;
import attini.action.actions.getdeployorigindata.dependencies.DependencyFacade;
import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stackdata.InitStackDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.domain.DeployOriginData;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

public class GetDeployOriginDataHandler {

    private static final Logger logger = Logger.getLogger(GetDeployOriginDataHandler.class);
    private final DeployOriginFacade deployOriginFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final CloudFormationClient cloudFormationClient;
    private final SendUsageDataFacade sendUsageDataFacade;
    private final InitStackDataFacade initStackDataFacade;
    private final ObjectMapper objectMapper;
    private final DeploymentPlanDataFacade deploymentPlanDataFacade;
    private final DependencyFacade dependencyFacade;

    public GetDeployOriginDataHandler(DeployOriginFacade deployOriginFacade,
                                      StepFunctionFacade stepFunctionFacade,
                                      CloudFormationClient cloudFormationClient,
                                      SendUsageDataFacade sendUsageDataFacade,
                                      InitStackDataFacade initStackDataFacade,
                                      ObjectMapper objectMapper, DeploymentPlanDataFacade deploymentPlanDataFacade,
                                      DependencyFacade dependencyFacade) {
        this.deployOriginFacade = requireNonNull(deployOriginFacade, "deployOriginFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.cloudFormationClient = requireNonNull(cloudFormationClient, "cloudFormationClient");
        this.sendUsageDataFacade = requireNonNull(sendUsageDataFacade, "sendUsageDataFacade");
        this.initStackDataFacade = requireNonNull(initStackDataFacade, "initStackDataFacade");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.deploymentPlanDataFacade = requireNonNull(deploymentPlanDataFacade, "deploymentPlanDataFacade");
        this.dependencyFacade = requireNonNull(dependencyFacade, "dependenciesFacade");
    }

    public Map<String, Object> getDeployOriginData(Map<String, Object> input) {

        GetDeployOriginDataRequest getDeployOriginDataRequest = objectMapper.convertValue(input, GetDeployOriginDataRequest.class);

        cancelPreviousExecutions(getDeployOriginDataRequest);

        DeploymentPlanStateData deploymentPlan = deploymentPlanDataFacade.getDeploymentPlan(getDeployOriginDataRequest.getSfnArn());

        final DeployOriginData deployOriginData = deployOriginFacade.getDeployOriginData(deploymentPlan.getObjectIdentifier(), deploymentPlan.getDeployOriginSourceName());

        if (stackIsRunning(deployOriginData.getStackName())) {
            //will cancel again if running after getting deploy data, its theoretically possible,
            // for example if several deployment are triggered and one is subjected to throttling
            logger.info("Stack is updating again, will abort");
            stepFunctionFacade.stopExecution(getDeployOriginDataRequest.getExecutionArn(),
                                             "stopped due to new stack update in progress");
        }
        sendUsageDataFacade.sendStartUsage(deployOriginData,
                                           getDeployOriginDataRequest.getExecutionArn());

        deployOriginFacade.setSfnExecutionArn(getDeployOriginDataRequest.getExecutionArn(),
                                              deployOriginData.getObjectIdentifier(),
                                              deploymentPlan.getDeployOriginSourceName());

        return Map.of("deploymentOriginData", deployOriginData,
                      "output", createOutput(deploymentPlan),
                      "dependencies", dependencyFacade.getDependencies(deployOriginData.getEnvironment(), deployOriginData.getDistributionName()),
                      "environment", deployOriginData.getEnvironment().asString(),
                      "stackParameters", initStackDataFacade.getInitStackParameters(deployOriginData.getStackName()),
                      "customData", input.get("customData"));


    }

    private HashMap<String, Object> createOutput(DeploymentPlanStateData deploymentPlan) {

        try {
            JsonNode defaults = objectMapper.readTree(deploymentPlan.getPayloadDefaults());

            if (!defaults.path("output").isMissingNode()){
                return objectMapper.convertValue(defaults.path("output"),new TypeReference<>(){});
            }
            return new HashMap<>();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void cancelPreviousExecutions(GetDeployOriginDataRequest getDeployOriginDataRequest) {

        stepFunctionFacade.listExecutions(getDeployOriginDataRequest.getSfnArn())
                          .filter(execution -> !execution.equals(getDeployOriginDataRequest.getExecutionArn()))
                          .peek(execution -> logger.info("canceling execution with executionArn = " + execution))
                          .forEach(execution -> stepFunctionFacade.stopExecution(execution,
                                                                                 "Stopped due to new execution started"));
    }


    private boolean stackIsRunning(String stackName) {
        return cloudFormationClient.describeStacks(DescribeStacksRequest.builder().stackName(stackName).build())
                                   .stacks()
                                   .get(0)
                                   .stackStatus().equals(StackStatus.UPDATE_IN_PROGRESS);
    }

}
