package attini.action.actions.runner;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import attini.action.actions.runner.input.RunnerInput;
import attini.action.facades.stackdata.ResourceStateDynamoFacade;
import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.domain.polling.Poller;
import attini.domain.polling.PollingResult;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class RunnerHandler {

    private static final Logger logger = Logger.getLogger(RunnerHandler.class);


    private final SqsClient sqsClient;
    private final EcsFacade ecsFacade;
    private final ResourceStateFacade resourceStateFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final ObjectMapper objectMapper;
    private final Ec2Facade ec2Facade;

    public RunnerHandler(SqsClient sqsClient,
                         EcsFacade ecsFacade,
                         ResourceStateDynamoFacade resourceStateFacade,
                         StepFunctionFacade stepFunctionFacade,
                         ObjectMapper objectMapper, Ec2Facade ec2Facade) {
        this.sqsClient = requireNonNull(sqsClient, "sqsClient");
        this.ecsFacade = requireNonNull(ecsFacade, "ecsFacade");
        this.resourceStateFacade = requireNonNull(resourceStateFacade, "resourceStateFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.ec2Facade = requireNonNull(ec2Facade, "ec2Facade");
    }

    public void handle(RunnerInput runnerInput) {

        try {
            logger.info("Attini runner triggered");

            String messageId = DigestUtils.md5Hex(runnerInput.deploymentPlanExecutionMetadata()
                                                             .executionArn() + runnerInput.deploymentPlanExecutionMetadata()
                                                                                          .stepName());

            RunnerData runnerData = resourceStateFacade.getRunnerData(runnerInput.deployOriginData().getStackName(),
                                                                      runnerInput.properties().runner())
                                                       .toBuilder()
                                                       .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                         .executionArn())
                                                       .build();

            sqsClient.sendMessage(SendMessageRequest.builder()
                                                    .queueUrl(runnerData.getTaskConfiguration().queueUrl())
                                                    .messageGroupId(messageId)
                                                    .messageDeduplicationId(messageId)
                                                    .messageBody(createInput(runnerInput,
                                                                             runnerData.currentConfigurationHash()))
                                                    .build());

            resourceStateFacade.saveRunnerData(runnerData);

            RunnerData runnerDataWithEc2Id = startEc2IfConfigured(runnerData);

            startNewTaskIfNotRunning(runnerDataWithEc2Id,
                                     runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                     runnerInput);

        } catch (AcquireEc2StartLockException e) {
            logger.info(
                    "Failed to acquire lock for starting the EC2 instance. Another step has started it and will assume responsibility for starting the ECS task");
        } catch (AcquireEcsStartLockException e) {
            logger.info("Failed to acquire lock for starting the ECS instance.");
        } catch (Ec2FailedToStartException e) {
            logger.error("EC2 instance did not start correctly", e);
            stepFunctionFacade.sendError(runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                         e.getMessage(),
                                         "Ec2StartFailedError");


        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument", e);
            stepFunctionFacade.sendError(runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                         e.getMessage(),
                                         "RunnerConfigError");
        }
    }

    private void startNewTaskIfNotRunning(RunnerData runnerData, String sfnToken, RunnerInput runnerInput) {
        try {
            runnerData.getTaskId()
                      .ifPresentOrElse(taskId -> {

                          TaskStatus taskStatus = ecsFacade.getTaskStatus(
                                  taskId, runnerData.getCluster());

                          if (taskStatus.isRunningOrStarting() && configurationHasChanged(runnerData)) {
                              logger.info("The task configuration has changed, will stop old task.");

                              runnerData.getEc2().ifPresentOrElse(ec2 -> {
                                  resourceStateFacade.saveRunnerData(runnerData.toBuilder()
                                                                               .shutdownHookDisabled(true)
                                                                               .build());
                                  ecsFacade.stopTask(taskId, runnerData.getCluster(), "Configuration change");
                                  logger.info(
                                          "Waiting for task to stop before starting new task to ensure EC2 instance is not terminated by the tasks shutdown hook");
                                  ecsFacade.waitUntilStopped(taskId, runnerData.getCluster());
                              }, () -> ecsFacade.stopTask(taskId, runnerData.getCluster(), "Configuration change"));

                              startTask(runnerData, sfnToken, runnerInput);
                          } else if (taskStatus.isStoppingOrStopped()) {
                              logger.info("Ecs task is stopped, will start new task.");
                              startTask(runnerData, sfnToken, runnerInput);
                          }
                      }, () -> {
                          logger.info("No previous task id found. Starting new task.");
                          startTask(runnerData, sfnToken, runnerInput);
                      });
        } catch (TaskStartFailedException e) {
            logger.error("Staring ECS Task failed async", e);
            runnerData.getEc2().flatMap(Ec2::getLatestEc2InstanceId).ifPresent(ec2Facade::terminateInstance);
            stepFunctionFacade.sendError(runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                         e.getTaskStatus().stopReason(),
                                         e.getTaskStatus().stopCode().map(Enum::name).orElse("unknown stop code"));
        } catch (TaskStartFailedSyncException e) {
            logger.error("Staring ECS Task failed sync", e);
            runnerData.getEc2().flatMap(Ec2::getLatestEc2InstanceId).ifPresent(ec2Facade::terminateInstance);
            stepFunctionFacade.sendError(runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                         e.getMessage(),
                                         "TaskFailedToStart");
        }


    }

    private boolean configurationHasChanged(RunnerData currentRunnerData) {
        return !currentRunnerData.previousConfigurationHash()
                                 .equals(currentRunnerData.currentConfigurationHash());
    }

    private String createInput(RunnerInput runnerInput, Integer currentConfigHash) {
        ObjectNode mutableInput = objectMapper.valueToTree(runnerInput);
        mutableInput.put("taskConfigHashCode", currentConfigHash);
        return mutableInput.toString();
    }


    private TaskStatus waitForStart(String taskId, String cluster, RunnerInput runnerInput) {
        TaskStatus taskStatus = ecsFacade.getTaskStatus(taskId, cluster);
        if (taskStatus.isDead()) {
            // sometimes, when you describe right after the task is started, it will return null,
            // in that case, try again
            waitFor(3);
            taskStatus = ecsFacade.getTaskStatus(taskId, cluster);
        }

        while (taskStatus.isStarting()) {
            logger.info("task is starting, status = " + taskStatus.lastStatus());
            waitFor(2);
            taskStatus = ecsFacade.getTaskStatus(taskId, cluster);
        }

        waitForRunnerStart(taskId, cluster, runnerInput);

        return taskStatus;
    }

    private void waitForRunnerStart(String taskId, String cluster, RunnerInput runnerInput) {
        logger.info("Waiting for runner to start");
        Poller.builder(() -> {
                  RunnerData runnerData1 = resourceStateFacade.getRunnerData(runnerInput.deployOriginData().getStackName(),
                                                                             runnerInput.properties().runner());


                  if (!runnerData1.getTaskId().map(s -> s.equals(taskId)).orElse(false)) {
                      logger.error("TaskId changed in states table. Will stop task");
                      ecsFacade.stopTask(taskId, cluster, "TaskId changed in state table");
                      return new PollingResult<>(true);
                  }

                  if (runnerData1.isStarted()) {
                      logger.info("Task started");
                      return new PollingResult<>(true);
                  }

                  TaskStatus taskStatus = ecsFacade.getTaskStatus(taskId, cluster);
                  if (taskStatus.isStoppingOrStopped()) {
                      throw new TaskStartFailedException(taskStatus);
                  }

                  return new PollingResult<>(false);
              }).setCalls(300)
              .setInterval(2, TimeUnit.SECONDS)
              .setTimeoutExceptionSupplier(() -> {
                  ecsFacade.stopTask(taskId, cluster, "Task did not start within the expected time frame");
                  return new IllegalStateException("Task did not start within the expected time frame");
              })
              .build()
              .poll();


    }

    private void waitFor(int time) {
        try {
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            logger.error("Sleep in polling was interrupted", e);
        }
    }

    private RunnerData startEc2IfConfigured(RunnerData runnerData) {
        return runnerData.getEc2()
                         .map(ec2 -> {
                             String instanceId = ec2.getLatestEc2InstanceId()
                                                    .map(s -> {
                                                        if (!ec2Facade.instanceIsRunning(s)) {
                                                            logger.info(
                                                                    "No ec2 instance is running. Will start new instance. ");
                                                            return ec2Facade.startInstance(ec2, runnerData);
                                                        }

                                                        if (ec2.getConfigHashCode() != ec2.getEc2Config().hashCode()) {
                                                            logger.info(
                                                                    "Ec2 config has changed. Will terminate old instance");
                                                            ec2Facade.terminateInstance(s);
                                                            ec2Facade.waitForStop(s);
                                                            return ec2Facade.startInstance(ec2, runnerData);
                                                        }
                                                        logger.info(
                                                                "Instance with correct config is still running. Will reuse.");
                                                        return s;
                                                    }).orElseGet(() -> ec2Facade.startInstance(ec2, runnerData));

                             RunnerData runnerDataWithEc2Id =
                                     runnerData.toBuilder()
                                               .ec2(ec2.toBuilder()
                                                       .latestEc2InstanceId(instanceId)
                                                       .configHashCode(ec2.getEc2Config().hashCode())
                                                       .build())
                                               .build();
                             resourceStateFacade.saveRunnerData(runnerDataWithEc2Id);
                             ec2Facade.waitForStart(instanceId, runnerData, ec2);
                             return runnerDataWithEc2Id;


                         }).orElse(runnerData);


    }

    private void startTask(RunnerData runnerData, String sfnToken, RunnerInput runnerInput) {
        logger.info("Starting new task");


        String taskArn = ecsFacade.startTask(runnerData,
                                             sfnToken);

        RunnerData updatedRunnerData = runnerData.toBuilder()
                                                 .taskId(taskArn)
                                                 .started(false)
                                                 .shutdownHookDisabled(false)
                                                 .build();

        resourceStateFacade.saveRunnerData(updatedRunnerData);

        TaskStatus taskStatus = waitForStart(taskArn, updatedRunnerData.getTaskConfiguration().cluster(), runnerInput);

        if (!taskStatus.isRunning()) {
            logger.error("Task failed to start");
            throw new TaskStartFailedException(taskStatus);
        }
    }
}
