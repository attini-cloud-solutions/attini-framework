package attini.action.actions.runner;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.ecs.model.ContainerDefinition;
import software.amazon.awssdk.services.ecs.model.ContainerOverride;
import software.amazon.awssdk.services.ecs.model.DescribeTaskDefinitionRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.InvalidParameterException;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import software.amazon.awssdk.services.ecs.model.LaunchType;
import software.amazon.awssdk.services.ecs.model.NetworkConfiguration;
import software.amazon.awssdk.services.ecs.model.PropagateTags;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.RunTaskResponse;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;
import software.amazon.awssdk.services.ecs.model.Task;
import software.amazon.awssdk.services.ecs.model.TaskOverride;

public class EcsFacade {

    private static final Logger logger = Logger.getLogger(EcsFacade.class);

    private final EcsClient ecsClient;
    private final EnvironmentVariables environmentVariables;

    public EcsFacade(EcsClient ecsClient, EnvironmentVariables environmentVariables) {
        this.ecsClient = requireNonNull(ecsClient, "ecsClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public TaskStatus getTaskStatus(String taskId, String cluster) {
        try {
            if (taskId == null) {
                return TaskStatus.deadTask();
            }
            DescribeTasksRequest.Builder builder = DescribeTasksRequest.builder()
                                                                       .tasks(taskId)
                                                                       .cluster(cluster);

            List<Task> tasks = ecsClient.describeTasks(builder.build()).tasks();
            if (tasks.isEmpty()) {
                return TaskStatus.deadTask();
            }
            Task task = tasks.get(0);

            return new TaskStatus(task.desiredStatus(), task.lastStatus(), task.stopCode(), task.stoppedReason());
        } catch (InvalidParameterException e) {
            logger.info(
                    "Invalid parameter when describing task. Most likely due to change of cluster. Will treat the task as dead. Message: " + e.getMessage());
            return TaskStatus.deadTask();
        }
    }

    public void stopTask(String taskId, String cluster) {
        StopTaskRequest.Builder builder = StopTaskRequest.builder()
                                                         .task(taskId)
                                                         .cluster(cluster)
                                                         .reason("Configuration change");

        ecsClient.stopTask(builder.build());

    }

    public String startTask(TaskConfiguration runnerConfig, String stackName, String runnerName, String sfnToken) {

        NetworkConfiguration networkConfiguration =
                NetworkConfiguration
                        .builder()
                        .awsvpcConfiguration(
                                AwsVpcConfiguration.builder()
                                                   .subnets(runnerConfig.subnets())
                                                   .securityGroups(runnerConfig.securityGroups())
                                                   .assignPublicIp(runnerConfig.assignPublicIp())
                                                   .build())
                        .build();


        RunTaskRequest.Builder builder = RunTaskRequest.builder()
                                                       .launchType(LaunchType.FARGATE)
                                                       .networkConfiguration(networkConfiguration)
                                                       .propagateTags(PropagateTags.TASK_DEFINITION)
                                                       .taskDefinition(runnerConfig.taskDefinitionArn())
                                                       .cluster(runnerConfig.cluster())
                                                       .overrides(getOverrides(runnerConfig,
                                                                               stackName,
                                                                               runnerName,
                                                                               sfnToken));


        RunTaskResponse runTaskResponse = ecsClient.runTask(builder.build());


        return runTaskResponse.tasks().get(0).taskArn();
    }

    private TaskOverride getOverrides(TaskConfiguration taskConfig,
                                      String stackName,
                                      String runnerName,
                                      String sfnToken) {

        String containerName = taskConfig.container().orElseGet(() -> {
            List<ContainerDefinition> containerDefinitions = ecsClient.describeTaskDefinition(
                                                                              DescribeTaskDefinitionRequest.builder()
                                                                                                           .taskDefinition(
                                                                                                                   taskConfig.taskDefinitionArn())
                                                                                                           .build())
                                                                      .taskDefinition()
                                                                      .containerDefinitions();

            if (containerDefinitions.size() != 1) {
                throw new IllegalArgumentException(
                        "Could not resolve container name. Container name is mandatory in the runner configuration if the number of containers in the task definition does not equal 1");
            }
            return containerDefinitions.get(0).name();
        });


        Set<KeyValuePair> variables =
                new HashSet<>(List.of(toEnvVariable("ATTINI_QUEUE_URL",
                                                    taskConfig.queueUrl()),
                                      toEnvVariable("ATTINI_DEPLOY_DATA_TABLE",
                                                    environmentVariables.getDeployOriginTableName()),
                                      toEnvVariable("ATTINI_RESOURCE_STATE_TABLE",
                                                    environmentVariables.getResourceStatesTableName()),
                                      toEnvVariable("ATTINI_CONFIGURATION_HASH",
                                                    String.valueOf(taskConfig.hashCode())),
                                      toEnvVariable("ATTINI_RUNNER_RESOURCE_NAME",
                                                    stackName + "-" + runnerName),
                                      toEnvVariable("ATTINI_DISABLE_ANSI_COLOR",
                                                    "true"),
                                      toEnvVariable("ATTINI_AWS_ACCOUNT", environmentVariables.getAccountId()),
                                      toEnvVariable("ATTINI_AWS_REGION", environmentVariables.getRegion())));


        taskConfig.getInstallationCommandsTimeout()
                  .ifPresent(integer -> variables.add(toEnvVariable("ATTINI_STARTUP_COMMANDS_TIMEOUT",
                                                                    integer)));

        taskConfig.runnerConfiguration()
                  .getIdleTimeToLive()
                  .ifPresent(integer ->
                                     variables.add(toEnvVariable(
                                             "ATTINI_RUNNER_IDLE_TTL",
                                             integer)));

        taskConfig.runnerConfiguration()
                  .getMaxConcurrentJobs()
                  .ifPresent(integer ->
                                     variables.add(toEnvVariable(
                                             "ATTINI_MAX_CONCURRENT_JOBS",
                                             integer)));

        taskConfig.runnerConfiguration()
                  .getJobTimeout()
                  .ifPresent(integer ->
                                     variables.add(toEnvVariable(
                                             "ATTINI_JOB_TIMEOUT",
                                             integer)));

        taskConfig.runnerConfiguration()
                  .getLogLevel()
                  .ifPresent(logLevel ->
                                     variables.add(toEnvVariable(
                                             "ATTINI_LOG_LEVEL",
                                             logLevel.name()))
                  );


        TaskOverride.Builder builder = TaskOverride.builder()
                                                   .containerOverrides(ContainerOverride.builder()
                                                                                        .name(containerName)
                                                                                        .command("/bin/bash",
                                                                                                 "-c",
                                                                                                 "attini-runner dry-run > /dev/null 2>&1; if [ $? -eq 127 ]; then echo \"attini-runner not found so installing it.\"; curl -sfL#o $HOME/attini-runner https://docs.attini.io/api/v1/runner/get-runner/$(uname -m)/$(uname -s)/1.2.6; chmod +x $HOME/attini-runner; $HOME/attini-runner '" + sfnToken + "'; else attini-runner '" + sfnToken + "'; fi;")
                                                                                        .environment(variables)
                                                                                        .build());

        taskConfig.getRoleArn().ifPresent(builder::taskRoleArn);
        return builder.build();
    }


    private KeyValuePair toEnvVariable(String name, String value) {
        return KeyValuePair.builder()
                           .name(name)
                           .value(value)
                           .build();
    }

    private KeyValuePair toEnvVariable(String name, Integer value) {
        return KeyValuePair.builder()
                           .name(name)
                           .value(String.valueOf(value))
                           .build();
    }
}
