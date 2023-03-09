/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import deployment.plan.system.EnvironmentVariables;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Disabled
@ExtendWith(MockitoExtension.class)
class TransformDeploymentPlanCloudFormationTest {


    @Mock
    EnvironmentVariables environmentVariables;

    @Mock
    Ec2Client ec2Client;

    @Mock
    DeploymentPlanStepsCreator deploymentPlanStepsCreator;

    ObjectMapper objectMapper = new ObjectMapper();

    TransformDeploymentPlanCloudFormation transformDeploymentPlanCloudFormation;


    @BeforeEach
    void setUp() {
        transformDeploymentPlanCloudFormation = new TransformDeploymentPlanCloudFormation(environmentVariables,
                                                                                          ec2Client,
                                                                                          objectMapper,
                                                                                          deploymentPlanStepsCreator);
    }

    @Test
    void transformDeploymentPlan_defaultRoleConfigured() throws IOException {

        when(environmentVariables.getDefaultRole()).thenReturn(
                "arn:aws:iam::655047308345:role/attini/attini-action-lambda-service-role-eu-west-1");
        Map<String, Object> input = getInput("DeploymentPlan_input.json");
        Map<String, Object> output = transformDeploymentPlanCloudFormation.transformTemplate(input);
        Map<String, Object> expectedOutput = getOutput();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode result = objectMapper.valueToTree(output);
        removeGuid(result);
        assertEquals(objectMapper.valueToTree(expectedOutput), result);
    }


    @Test
    void transformDeploymentPlan_withRoleInInput() throws IOException {
        Map<String, Object> input = getInput("DeploymentPlan_input_with_role.json");
        Map<String, Object> output = transformDeploymentPlanCloudFormation.transformTemplate(input);
        Map<String, Object> expectedOutput = getOutput();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode result = objectMapper.valueToTree(output);
        removeGuid(result);

        assertEquals(objectMapper.valueToTree(expectedOutput), result);
    }

    @Test
    void transformDeploymentPlan_parallel() throws IOException {
        Map<String, Object> input = getInput("DeploymentPlan_input_parallel.json");
        Map<String, Object> output = transformDeploymentPlanCloudFormation.transformTemplate(input);
        Map<String, Object> expectedOutput = getParallelOutput();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode result = objectMapper.valueToTree(output);
        removeGuid(result);

        assertEquals(objectMapper.valueToTree(expectedOutput), result);
    }

    private void removeGuid(JsonNode result) {
        ObjectNode jsonNodes = (ObjectNode) result.get("fragment")
                                                  .get("Resources")
                                                  .get("AttiniDeploymentPlanSfnMyDeploymentPlanTrigger")
                                                  .get("Properties");

        jsonNodes.remove("RandomGUID");
    }

    private Map<String, Object> getInput(String fileName) throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputFilePath.toFile(), new TypeReference<>() {
        });
    }

    private Map<String, Object> getParallelOutput() throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", "DeploymentPlan_output_parallel.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputFilePath.toFile(), new TypeReference<>() {
        });
    }

    private Map<String, Object> getOutput() throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", "DeploymentPlan_output.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputFilePath.toFile(), new TypeReference<>() {
        });
    }
}
