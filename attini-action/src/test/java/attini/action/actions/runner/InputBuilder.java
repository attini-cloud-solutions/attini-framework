package attini.action.actions.runner;

import java.util.Collections;
import java.util.List;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.action.actions.runner.input.RunnerInput;
import attini.action.actions.runner.input.RunnerProperties;
import attini.domain.DeployOriginDataTestBuilder;

public class InputBuilder {


    public static RunnerInput aRunnerInput() {
        RunnerProperties runnerProperties = new RunnerProperties(List.of("echo never gonna give you up",
                                                                         "echo never gonna let you down"),
                                                                 "my-runner",
                                                                 null);


        DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata = new DeploymentPlanExecutionMetadata(0,
                                                                                                              "the-sfn-token",
                                                                                                              "my-runner-step",
                                                                                                              SfnExecutionArn.create("the-execution-arn"),
                                                                                                              "2022-04-28T11:58:00.019Z");


        return new RunnerInput(Collections.emptyMap(),
                               runnerProperties,
                               deploymentPlanExecutionMetadata,
                               DeployOriginDataTestBuilder.aDeployOriginData().build(),
                               Collections.emptyMap(),
                               Collections.emptyMap(),
                               Collections.emptyMap());
    }
}
