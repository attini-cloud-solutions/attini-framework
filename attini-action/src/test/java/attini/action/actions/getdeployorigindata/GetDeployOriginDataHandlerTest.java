/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.getdeployorigindata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.UPDATE_IN_PROGRESS;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.SendUsageDataFacade;
import attini.action.builders.TestBuilders;
import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stackdata.DistributionDataFacade;
import attini.action.facades.stackdata.InitStackDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.domain.DeployOriginData;
import attini.domain.DeployOriginDataTestBuilder;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;

@ExtendWith(MockitoExtension.class)
class GetDeployOriginDataHandlerTest {

    @Mock
    DeployOriginFacade deployOriginFacade;

    @Mock
    StepFunctionFacade stepFunctionFacade;

    @Mock
    CloudFormationClient cloudFormationClient;
    @Mock
    SendUsageDataFacade sendUsageDataFacade;

    @Mock
    DistributionDataFacade distributionDataFacade;

    @Mock
    InitStackDataFacade initStackDataFacade;

    @Mock
    DeploymentPlanDataFacade deploymentPlanDataFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );


    GetDeployOriginDataHandler getDeployOriginDataHandler;

    @BeforeEach
    void setUp() {
        getDeployOriginDataHandler = new GetDeployOriginDataHandler(deployOriginFacade,
                                                                    stepFunctionFacade,
                                                                    cloudFormationClient,
                                                                    sendUsageDataFacade,
                                                                    distributionDataFacade,
                                                                    initStackDataFacade,
                                                                    objectMapper, deploymentPlanDataFacade);
    }


    @Test
    void getDeployOriginData() throws IOException {
        Map<String, Object> input = getInput("get-deploy-origin-data-request.json");
        String sfnArn = input.get("sfnArn").toString();
        when(stepFunctionFacade.listExecutions(sfnArn)).thenReturn(Stream.of(
                "some-arn-1123232323232"));

        DeployOriginData deployDatMockData = DeployOriginDataTestBuilder.aDeployOriginData().build();
        DeploymentPlanStateData deploymentPlanStateData = new DeploymentPlanStateData("dev-infra", ObjectIdentifier.of("123231232"), "{}");
        when(deploymentPlanDataFacade.getDeploymentPlan(sfnArn)).thenReturn(deploymentPlanStateData);
        when(deployOriginFacade.getDeployOriginData(deploymentPlanStateData.getObjectIdentifier(), deploymentPlanStateData.getDeployOriginSourceName())).thenReturn(deployDatMockData);
        when(distributionDataFacade.getDistribution(any(DistributionName.class), any(Environment.class))).thenReturn(
                TestBuilders.aDistribution().build());
        String stackName = deployDatMockData.getStackName();

        when(cloudFormationClient.describeStacks(DescribeStacksRequest.builder()
                                                                      .stackName(stackName)
                                                                      .build()))
                .thenReturn(aDescribeStackResponse(stackName, CREATE_COMPLETE));

        Map<String, Object> deployOriginData = getDeployOriginDataHandler.getDeployOriginData(input);

        verify(stepFunctionFacade, times(1)).stopExecution(anyString(), anyString());
        assertTrue(deployOriginData.containsKey("deploymentOriginData"));
        assertFalse(deployOriginData.containsKey("attiniActionType"));

    }

    @Test
    void getDeployOriginData_shouldCancelExecutionsIfUpdateInProgress() throws IOException {
        Map<String, Object> input = getInput("get-deploy-origin-data-request.json");
        String sfnArn = input.get("sfnArn").toString();
        when(stepFunctionFacade.listExecutions(sfnArn)).thenReturn(Stream.of(
                "some-arn-1123232323232"));
        DeployOriginData deployDatMockData = DeployOriginDataTestBuilder.aDeployOriginData().build();
        DeploymentPlanStateData deploymentPlanStateData = new DeploymentPlanStateData("dev-infra", ObjectIdentifier.of("123231232"), "{}");
        when(deploymentPlanDataFacade.getDeploymentPlan(sfnArn)).thenReturn(deploymentPlanStateData);

        when(deployOriginFacade.getDeployOriginData(deploymentPlanStateData.getObjectIdentifier(), deploymentPlanStateData.getDeployOriginSourceName())).thenReturn(deployDatMockData);
        when(distributionDataFacade.getDistribution(any(DistributionName.class), any(Environment.class))).thenReturn(
                TestBuilders.aDistribution().build());
        String stackName = deployDatMockData.getStackName();

        when(cloudFormationClient.describeStacks(DescribeStacksRequest.builder()
                                                                      .stackName(stackName)
                                                                      .build()))
                .thenReturn(aDescribeStackResponse(stackName, UPDATE_IN_PROGRESS));

        Map<String, Object> deployOriginData = getDeployOriginDataHandler.getDeployOriginData(input);
        verify(stepFunctionFacade, times(2)).stopExecution(anyString(), anyString());

        assertTrue(deployOriginData.containsKey("deploymentOriginData"));
        assertFalse(deployOriginData.containsKey("attiniActionType"));

    }

    private DescribeStacksResponse aDescribeStackResponse(String stackName, StackStatus stackStatus) {
        return DescribeStacksResponse.builder()
                                     .stacks(Stack.builder()
                                                  .stackName(stackName)
                                                  .stackStatus(stackStatus)
                                                  .build())
                                     .build();
    }

    private static Map<String, Object> getInput(String fileName) throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputFilePath.toFile(), new TypeReference<>() {
        });
    }
}
