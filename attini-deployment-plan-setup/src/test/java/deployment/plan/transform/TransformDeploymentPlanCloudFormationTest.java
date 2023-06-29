/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import deployment.plan.system.EnvironmentVariables;
import deployment.plan.transform.simplesyntax.TransformSimpleSyntax;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.ec2.Ec2Client;

@QuarkusTest
class TransformDeploymentPlanCloudFormationTest {


    @InjectMock
    EnvironmentVariables environmentVariables;

    @InjectMock
    DeploymentPlanStepsCreator deploymentPlanStepsCreator;

    @InjectMock
    Ec2Client ec2Client;

    @Inject
    ObjectMapper objectMapper;

    TransformDeploymentPlanCloudFormation transformDeploymentPlanCloudFormation;


    @BeforeEach
    void setUp() {
        when(environmentVariables.getRegion()).thenReturn("eu-west-1");
        when(deploymentPlanStepsCreator.createDefinition(any())).thenReturn(new DeploymentPlanDefinition(
                Collections.emptyMap(), Collections.emptyList()));
        transformDeploymentPlanCloudFormation = new TransformDeploymentPlanCloudFormation(environmentVariables,
                                                                                          ec2Client,
                                                                                          objectMapper,
                                                                                          deploymentPlanStepsCreator, new TransformSimpleSyntax(objectMapper));

    }

    @Test
    void transformDeploymentPlan_defaultRoleConfigured() throws IOException {
      //  QuarkusMock.installMockForInstance(ec2Client, Ec2Client.class);
        when(environmentVariables.getDefaultRole()).thenReturn(
                "arn:aws:iam::655047308345:role/attini/attini-action-lambda-service-role-eu-west-1");
        Map<String, Object> input = getInput("DeploymentPlan_input.json");
        Map<String, Object> output = transformDeploymentPlanCloudFormation.transformTemplate(input);
        Map<String, Object> expectedOutput = getOutput();
        JsonNode result = objectMapper.valueToTree(output);
        removeGuid(result);

        assertEquals(objectMapper.valueToTree(expectedOutput), result);
    }


    @Test
    void transformDeploymentPlan_withRoleInInput() throws IOException {
        Map<String, Object> input = getInput("DeploymentPlan_input_with_role.json");
        Map<String, Object> output = transformDeploymentPlanCloudFormation.transformTemplate(input);
        Map<String, Object> expectedOutput = getOutput();
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

    private Map<String, Object> getOutput() throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", "DeploymentPlan_output.json");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputFilePath.toFile(), new TypeReference<>() {
        });
    }
}
