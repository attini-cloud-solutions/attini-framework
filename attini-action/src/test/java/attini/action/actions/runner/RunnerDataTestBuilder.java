package attini.action.actions.runner;

import java.util.Set;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;

public class RunnerDataTestBuilder {

    public static RunnerData.RunnerDataBuilder aRunnerData() {
        TaskConfiguration taskConfiguration = aTaskConfiguration().build();
        return RunnerData.builder()
                         .taskId("taskId")
                         .cluster("a-cluster")
                         .runnerName("my-runner")
                         .distributionName(DistributionName.of("infra-dist"))
                         .stackName("my-stack")
                         .distributionId(DistributionId.of("12323"))
                         .taskConfigurationHashCode(taskConfiguration.hashCode())
                         .container("my-container")
                         .environment(Environment.of("dev"))
                         .objectIdentifier("an-id")
                         .taskDefinitionArn("my-task-def").taskConfiguration(taskConfiguration);
    }

    public static TaskConfiguration.TaskConfigurationBuilder aTaskConfiguration() {
        return TaskConfiguration.builder()
                                .assignPublicIp("ENABLED")
                                .taskDefinitionArn("some-arn")
                                .securityGroups(Set.of("1"))
                                .subnets(Set.of("2", "3", "4"))
                                .container("my-container")
                                .cluster("a-cluster")
                                .queueUrl("some-queue")
                                .attiniVersion("1.2.0")
                                .runnerConfiguration(aRunnerConfiguration().build());
    }

    public static RunnerConfiguration.RunnerConfigurationBuilder aRunnerConfiguration() {
        return RunnerConfiguration.builder().jobTimeout(600).maxConcurrentJobs(5).idleTimeToLive(600);
    }


}
