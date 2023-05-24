package attini.action.actions.runner;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.ecs.model.ContainerDefinition;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;
import software.amazon.awssdk.services.ecs.model.ContainerInstanceStatus;
import software.amazon.awssdk.services.ecs.model.ContainerOverride;
import software.amazon.awssdk.services.ecs.model.DescribeContainerInstancesRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTaskDefinitionRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.EcsException;
import software.amazon.awssdk.services.ecs.model.InvalidParameterException;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import software.amazon.awssdk.services.ecs.model.LaunchType;
import software.amazon.awssdk.services.ecs.model.ListContainerInstancesRequest;
import software.amazon.awssdk.services.ecs.model.NetworkConfiguration;
import software.amazon.awssdk.services.ecs.model.PlacementConstraint;
import software.amazon.awssdk.services.ecs.model.PlacementConstraintType;
import software.amazon.awssdk.services.ecs.model.PropagateTags;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.RunTaskResponse;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;
import software.amazon.awssdk.services.ecs.model.Task;
import software.amazon.awssdk.services.ecs.model.TaskOverride;

public class EcsFacade {

    private static final Logger logger = Logger.getLogger(EcsFacade.class);

    private static final String RUNNER_VERSION = "2.0.1";

    private static final String RUNNER_REQUIRED_VERSION = "2.0.1";

    private final EcsClient ecsClient;
    private final EnvironmentVariables environmentVariables;
    private final ResourceStateFacade resourceStateFacade;

    public EcsFacade(EcsClient ecsClient,
                     EnvironmentVariables environmentVariables,
                     ResourceStateFacade resourceStateFacade) {
        this.ecsClient = requireNonNull(ecsClient, "ecsClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.resourceStateFacade = requireNonNull(resourceStateFacade, "resourceStateFacade");
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

    public void stopTask(String taskId, String cluster, String stopReason) {
        StopTaskRequest.Builder builder = StopTaskRequest.builder()
                                                         .task(taskId)
                                                         .cluster(cluster)
                                                         .reason(stopReason);

        ecsClient.stopTask(builder.build());

    }

    public void waitUntilStopped(String taskId, String cluster) {
        ecsClient.waiter().waitUntilTasksStopped(DescribeTasksRequest.builder().cluster(cluster).tasks(taskId).build());
    }

    public boolean isRegisterWithCluster(String instanceId, RunnerData runnerData) {

        logger.info("Checking if instance is registered");

        List<String> containerInstances = ecsClient.listContainerInstances(
                                                           ListContainerInstancesRequest.builder()
                                                                                        .status(ContainerInstanceStatus.ACTIVE)
                                                                                        .filter("ec2InstanceId == " + instanceId)
                                                                                        .cluster(
                                                                                                runnerData.getTaskConfiguration()
                                                                                                          .cluster())
                                                                                        .build())
                                                   .containerInstanceArns();
        if (containerInstances.isEmpty()) {
            return false;
        }
        return ecsClient
                .describeContainerInstances(DescribeContainerInstancesRequest.builder()
                                                                             .containerInstances(containerInstances)
                                                                             .cluster(runnerData.getTaskConfiguration()
                                                                                                .cluster())
                                                                             .build())
                .containerInstances()
                .stream()
                .filter(ContainerInstance::agentConnected)
                .anyMatch(containerInstance -> instanceId.equals(containerInstance.ec2InstanceId()));

    }

    public String startTask(RunnerData runnerData,
                            String sfnToken) {

        try {

          if (!resourceStateFacade.acquireEcsStartLock(runnerData)){
              throw new AcquireEcsStartLockException();
          }

            TaskConfiguration runnerConfig = runnerData.getTaskConfiguration();

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

            RunTaskResponse runTaskResponse =
                    ecsClient.runTask(createRunTaskRequest(runnerData,
                                                           sfnToken,
                                                           runnerConfig,
                                                           networkConfiguration));

            if (!runTaskResponse.failures().isEmpty()) {
                logger.error("Ecs tasks failed to start");
                runTaskResponse.failures().forEach(logger::error);
            }

            return runTaskResponse.tasks()
                                  .stream()
                                  .findAny()
                                  .map(Task::taskArn)
                                  .orElseThrow(() -> new TaskStartFailedSyncException(
                                          "Ecs task failed to start. Reason: " + runTaskResponse.failures()));
        } catch (EcsException e) {
            throw new TaskStartFailedSyncException(e.getMessage(), e);
        }
    }

    private RunTaskRequest createRunTaskRequest(RunnerData runnerData,
                                                String sfnToken,
                                                TaskConfiguration runnerConfig,
                                                NetworkConfiguration networkConfiguration) {
        RunTaskRequest.Builder builder = RunTaskRequest.builder()
                                                       .propagateTags(PropagateTags.TASK_DEFINITION)
                                                       .taskDefinition(runnerConfig.taskDefinitionArn())
                                                       .cluster(runnerConfig.cluster())
                                                       .overrides(getOverrides(runnerData,
                                                                               sfnToken));


        if (runnerData.getEc2().isPresent()) {
            return builder.placementConstraints(PlacementConstraint.builder()
                                                                   .type(PlacementConstraintType.MEMBER_OF)
                                                                   .expression("attribute:runnerResourceName == " + runnerData.getAttiniRunnerResourceName())
                                                                   .build())
                          .launchType(LaunchType.EC2)
                          .enableECSManagedTags(true)
                          .build();
        }

        //  Network configuration is set on the ECS task if the launch type is fargate.
        //  Otherwise, it is configured on the EC2 instance.
        return builder.networkConfiguration(networkConfiguration)
                      .launchType(LaunchType.FARGATE)
                      .build();
    }

    private TaskOverride getOverrides(RunnerData runnerData,
                                      String sfnToken) {

        TaskConfiguration taskConfig = runnerData.getTaskConfiguration();

        String containerName = taskConfig.container()
                                         .orElseGet(() -> {
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
                                                    String.valueOf(runnerData.currentConfigurationHash())),
                                      toEnvVariable("ATTINI_RUNNER_RESOURCE_NAME",
                                                    runnerData.getAttiniRunnerResourceName()),
                                      toEnvVariable("ATTINI_DISABLE_ANSI_COLOR",
                                                    "true"),
                                      toEnvVariable("ATTINI_AWS_ACCOUNT", environmentVariables.getAccountId()),
                                      toEnvVariable("ATTINI_AWS_REGION", environmentVariables.getRegion()),
                                      toEnvVariable("ATTINI_ARTIFACT_STORE",
                                                    environmentVariables.getAttiniArtifactBucket())));


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

        runnerData.getEc2()
                  .flatMap(Ec2::getLatestEc2InstanceId)
                  .ifPresent(s -> variables.add(toEnvVariable("ATTINI_EC2_INSTANCE_ID", s)));


        ContainerOverride.Builder containterOverridesBuilder = ContainerOverride.builder()
                                                                                .name(containerName)
                                                                                .command("/bin/bash",
                                                                                         "-c",
                                                                                         getStartupCommand(sfnToken))
                                                                                .environment(variables);


        TaskOverride.Builder builder = TaskOverride.builder()
                                                   .containerOverrides(containterOverridesBuilder
                                                                               .build());

        taskConfig.cpu().ifPresent(cpu -> builder.cpu(String.valueOf(cpu)));
        taskConfig.memory().ifPresent(memory -> builder.memory(String.valueOf(memory)));
        taskConfig.roleArn().ifPresent(builder::taskRoleArn);
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

    private String getStartupCommand(String sfnToken) {
        return """      
                set -u
                set -e
                set -o pipefail
                                
                error(){
                  echo "ERROR  $1" >&2
                }
                                
                info(){
                  echo "INFO   $1"
                }
                                
                hint(){
                  echo "HINT   $1"
                }
                                
                OS=$(uname)
                CPU=$(uname -m)
                ATTINI_SFN_TOKEN=%s
                ATTINI_REQUIRED_RUNNER_VERSION=%s
                                
                if [[ "${OS}" != "Linux"  ]]
                then
                  error "Attini runner is only supported on Linux and you are running [ ${OS} ]"
                  exit 1
                fi
                                
                if [[ "${CPU}" != "x86_64" && "${CPU}" != "aarch64" && "${CPU}" != "arm64"  ]]
                then
                  error "Attini runner is only supported on CPU architectures x86_64 and aarch64 (arm64) and you are using [ ${CPU} ] "
                  exit 1
                fi
                         
                if [[ $(attini-runner dry-run > /dev/null 2>&1; echo $?) = "127" ]]
                then
                  info "attini-runner not found so installing it."
                  curl -sfL#o $HOME/attini-runner https://docs.attini.io/api/v1/runner/get-runner/${CPU}/${OS}/%s
                  chmod +x $HOME/attini-runner
                  export ATTINI_RUNNER_EXEC=$HOME/attini-runner
                  exec $HOME/attini-runner $ATTINI_SFN_TOKEN $ATTINI_REQUIRED_RUNNER_VERSION
                else
                  export ATTINI_RUNNER_EXEC=attini-runner
                  exec attini-runner $ATTINI_SFN_TOKEN $ATTINI_REQUIRED_RUNNER_VERSION
                fi
                """.formatted(sfnToken, RUNNER_REQUIRED_VERSION, RUNNER_VERSION);
    }
}
