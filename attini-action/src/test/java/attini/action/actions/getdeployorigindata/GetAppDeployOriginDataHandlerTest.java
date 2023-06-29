/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.getdeployorigindata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.actions.getdeployorigindata.dependencies.DependencyFacade;
import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.deployorigin.DeploymentName;
import attini.action.facades.stackdata.AppDeploymentPlanDataFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.domain.DeployOriginData;
import attini.domain.DeployOriginDataTestBuilder;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

@ExtendWith(MockitoExtension.class)
class GetAppDeployOriginDataHandlerTest {

    @Mock
    DeployOriginFacade deployOriginFacade;

    @Mock
    StepFunctionFacade stepFunctionFacade;


    @Mock
    AppDeploymentPlanDataFacade appDeploymentPlanDataFacade;

    @Mock
    DeploymentPlanDataFacade deploymentPlanDataFacade;

    @Mock
    DependencyFacade dependencyFacade;

    private final ObjectMapper objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);


    GetAppDeployOriginDataHandler getDeployOriginDataHandler;

    @BeforeEach
    void setUp() {
        getDeployOriginDataHandler = new GetAppDeployOriginDataHandler(deployOriginFacade,
                                                                    stepFunctionFacade,
                                                                    objectMapper,
                                                                    deploymentPlanDataFacade,
                                                                    appDeploymentPlanDataFacade,
                                                                       dependencyFacade);
    }


    @Test
    void getDeployOriginData() throws IOException {
        Map<String, Object> input = getInput();
        String sfnArn = input.get("sfnArn").toString();
        when(stepFunctionFacade.listExecutions(sfnArn)).thenReturn(Stream.of(
                "some-arn-1123232323232"));

        DeployOriginData deployOriginData = DeployOriginDataTestBuilder.aDeployOriginData().build();
        DeployOriginData parentDeployOriginData = DeployOriginDataTestBuilder.aDeployOriginData().distributionName(DistributionName.of("parent")).build();

        Environment env = Environment.of("dev");
        DeploymentPlanStateData deploymentPlanStateData = new DeploymentPlanStateData(DeploymentName.create(env, DistributionName.of("infra")),
                                                                                      ObjectIdentifier.of("123231232"),
                                                                                      "{}",
                                                                                      env);
        when(deploymentPlanDataFacade.getDeploymentPlan(sfnArn)).thenReturn(deploymentPlanStateData);

        when(deployOriginFacade.getDeployOriginData(deploymentPlanStateData.getObjectIdentifier(),
                                                    deploymentPlanStateData.getDeployOriginSourceName()))
                .thenReturn(deployOriginData);

        when(deployOriginFacade.getDeployOriginData(ObjectIdentifier.of("parent-id"),
                                                    DeploymentName.create(env, DistributionName.of("infra-parent"))))
                .thenReturn(parentDeployOriginData);


        when(dependencyFacade.getDependencies(deployOriginData.getEnvironment(), deployOriginData.getDistributionName()))
                .thenReturn(Collections.emptyMap());



        Map<String, Object> result = getDeployOriginDataHandler.getAppDeployOriginData(input);

        assertTrue(result.containsKey("deploymentOriginData"));
        assertFalse(result.containsKey("attiniActionType"));

    }

    @Test
    void getDeployOriginData_shouldCancelExecutionsIfUpdateInProgress() throws IOException {
        Map<String, Object> input = getInput();
        String sfnArn = input.get("sfnArn").toString();
        String prevArn = "some-arn-1123232323232";
        when(stepFunctionFacade.listExecutions(sfnArn)).thenReturn(Stream.of(
                prevArn));
        DeployOriginData deployOriginData = DeployOriginDataTestBuilder.aDeployOriginData().build();
        DeployOriginData parentDeployOriginData = DeployOriginDataTestBuilder.aDeployOriginData().distributionName(DistributionName.of("parent")).build();

        Environment env = Environment.of("dev");
        DeploymentName deploymentName = DeploymentName.create(env, DistributionName.of("infra"));
        DeploymentPlanStateData deploymentPlanStateData = new DeploymentPlanStateData(deploymentName,
                                                                                      ObjectIdentifier.of("123231232"),
                                                                                      "{}", env);
        when(deploymentPlanDataFacade.getDeploymentPlan(sfnArn)).thenReturn(deploymentPlanStateData);

        when(deployOriginFacade.getDeployOriginData(deploymentPlanStateData.getObjectIdentifier(),
                                                    deploymentPlanStateData.getDeployOriginSourceName()))
                .thenReturn(deployOriginData);

        when(deployOriginFacade.getDeployOriginData(ObjectIdentifier.of("parent-id"),
                                                    DeploymentName.create(env, DistributionName.of("infra-parent"))))
                .thenReturn(parentDeployOriginData);

        when(dependencyFacade.getDependencies(deployOriginData.getEnvironment(), deployOriginData.getDistributionName())).thenReturn(
                Collections.emptyMap());

        when(deployOriginFacade.getLatestExecutionArns(deploymentName)).thenReturn(Set.of(prevArn));
        Map<String, Object> result = getDeployOriginDataHandler.getAppDeployOriginData(input);
        verify(stepFunctionFacade, times(1)).stopExecution(anyString(), anyString());

        assertTrue(result.containsKey("deploymentOriginData"));
        assertFalse(result.containsKey("attiniActionType"));

    }

    private Map<String, Object> getInput() throws IOException {
        String input = """
                {
                    "attiniActionType": "GetAppDeployOriginData",
                    "appConfig": {
                        "some-value": "Bye!",
                        "samPath": "python-sam-app",
                        "cdkPath": "./typescript-cdk-app"
                    },
                    "distributionName": "infra",
                    "platformDistributionName": "infra-parent",
                    "platformDistributionIdentifier": "parent-id",
                    "payload": {
                        "appConfig": {
                            "some-value": "Bye!",
                            "samPath": "python-sam-app",
                            "cdkPath": "./typescript-cdk-app"
                        },
                        "distributionName": "infra",
                        "objectIdentifier": "123231232",
                        "appDeploymentPlan": "MicroServices",
                        "platformDistributionName": "infra-parent",
                        "platformDistributionIdentifier": "parent-id"
                    },
                    "executionArn": "arn:aws:states:eu-west-1:855066048591:execution:AttiniDeploymentPlanSfnAppDeploymentPlan-pYUC7YR89iym:e24dfbb9-2477-42ac-b273-f04ffccad921",
                    "objectIdentifier": "123231232",
                    "sfnArn": "arn:aws:states:eu-west-1:855066048591:stateMachine:AttiniDeploymentPlanSfnAppDeploymentPlan-pYUC7YR89iym"
                }
                """;
        return objectMapper.readValue(input, new TypeReference<>() {
        });
    }
}
