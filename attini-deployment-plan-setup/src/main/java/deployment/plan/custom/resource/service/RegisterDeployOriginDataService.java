/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import deployment.plan.transform.AttiniStep;
import deployment.plan.transform.Runner;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;

public class RegisterDeployOriginDataService {
    private static final Logger logger = Logger.getLogger(RegisterDeployOriginDataService.class);
    private final DeployStatesFacade deployStatesFacade;
    private final CloudFormationClient cloudFormationClient;


    public RegisterDeployOriginDataService(DeployStatesFacade deployStatesFacade,
                                           CloudFormationClient cloudFormationClient) {
        this.deployStatesFacade = requireNonNull(deployStatesFacade, "deployStatesFacade");
        this.cloudFormationClient = requireNonNull(cloudFormationClient, "cloudFormationClient");
    }

    public void registerDeployOriginData(RegisterDeployOriginDataRequest request) {

        switch (request.getCfnRequestType()) {
            case CREATE -> {
                logger.info("Register OriginDeployDataLink");
                DeploymentPlanResourceState dataSource = createDataSource(request);
                deployStatesFacade.saveDeploymentPlanState(dataSource);
                request.getRunners().forEach(runner -> deployStatesFacade.saveRunnerState(runner, dataSource));
                saveToInitStack(request);

            }
            case UPDATE -> {
                logger.info("Updating OriginDeployDataLink");
                deployStatesFacade.deleteDeploymentPlanState(request.getOldSfnArn());
                DeploymentPlanResourceState dataSourceUpdate = createDataSource(request);
                logger.info("working with data source = " + dataSourceUpdate);
                deployStatesFacade.saveDeploymentPlanState(dataSourceUpdate);
                request.getRunners().forEach(runner -> deployStatesFacade.saveRunnerState(runner, dataSourceUpdate));
                saveToInitStack(request);

            }
            case DELETE -> deployStatesFacade.deleteDeploymentPlanState(request.getNewSfnArn());
            default -> throw new IllegalStateException("Invalid request type = " + request.getCfnRequestType());
        }

    }

    private void saveToInitStack(RegisterDeployOriginDataRequest request) {

        deployStatesFacade.saveToInitData(request.getRunners()
                                                 .stream()
                                                 .map(Runner::getName)
                                                 .collect(Collectors.toList()),
                                          request.getStackName(), request.getParameters());
    }


    private DeploymentPlanResourceState createDataSource(RegisterDeployOriginDataRequest request) {
        InitStackResourceState initStackState = deployStatesFacade.getInitStackState(request.getStackName());
        return DeploymentPlanResourceState.builder()
                                          .attiniDistributionName(initStackState.getDistributionName())
                                          .attiniEnvironmentName(initStackState.getEnvironment())
                                          .attiniObjectIdentifier(initStackState.getObjectIdentifier())
                                          .attiniDistributionId(initStackState.getDistributionId())
                                          .sfnArn(request.getNewSfnArn())
                                          .stackName(request.getStackName())
                                          .attiniSteps(getAttiniSteps(request))
                                          .payloadDefaults(request.getPayloadDefaults())
                                          .build();


    }

    private List<AttiniStep> getAttiniSteps(RegisterDeployOriginDataRequest request) {
        DescribeStackResourceResponse response = cloudFormationClient.describeStackResource(DescribeStackResourceRequest.builder()
                                                                                                                        .stackName(
                                                                                                                                request.getStackName())
                                                                                                                        .logicalResourceId(
                                                                                                                                request.getStepFunctionLogicalId())
                                                                                                                        .build());


        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(response.stackResourceDetail().metadata());
            JsonNode attiniSteps = jsonNode.path("AttiniSteps");
            if (attiniSteps.isMissingNode()) {
                return Collections.emptyList();
            }
            ObjectReader reader = objectMapper.readerFor(new TypeReference<List<AttiniStep>>() {
            });
            return reader.readValue(attiniSteps);
        } catch (IOException e) {
            logger.error("Could not parse resource metadata", e);
            throw new RuntimeException("Could not parse resource metadata", e);
        }
    }

}
