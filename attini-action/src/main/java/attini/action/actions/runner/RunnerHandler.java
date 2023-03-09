package attini.action.actions.runner;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import attini.action.actions.runner.input.RunnerInput;
import attini.action.facades.stackdata.StackDataDynamoFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class RunnerHandler {

    private static final Logger logger = Logger.getLogger(RunnerHandler.class);


    private final SqsClient sqsClient;
    private final EcsFacade ecsFacade;
    private final StackDataDynamoFacade stackDataDynamoFacade;
    private final StepFunctionFacade stepFunctionFacade;
    private final ObjectMapper objectMapper;

    public RunnerHandler(SqsClient sqsClient,
                         EcsFacade ecsFacade,
                         StackDataDynamoFacade stackDataDynamoFacade,
                         StepFunctionFacade stepFunctionFacade,
                         ObjectMapper objectMapper) {
        this.sqsClient = requireNonNull(sqsClient, "sqsClient");
        this.ecsFacade = requireNonNull(ecsFacade, "ecsFacade");
        this.stackDataDynamoFacade = requireNonNull(stackDataDynamoFacade, "stackDataDynamoFacade");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.objectMapper = objectMapper;
    }

    public void handle(RunnerInput runnerInput) {

        try {
            logger.info("Attini runner triggered");

            String messageId = DigestUtils.md5Hex(runnerInput.deploymentPlanExecutionMetadata()
                                                             .executionArn() + runnerInput.deploymentPlanExecutionMetadata()
                                                                                          .stepName());

            RunnerData runnerData = stackDataDynamoFacade.getRunnerData(runnerInput.deployOriginData().getStackName(),
                                                                        runnerInput.properties().runner());
            sqsClient.sendMessage(SendMessageRequest.builder()
                                                    .queueUrl(runnerData.getTaskConfiguration().queueUrl())
                                                    .messageGroupId(messageId)
                                                    .messageDeduplicationId(messageId)
                                                    .messageBody(createInput(runnerInput,
                                                                             runnerData.getTaskConfiguration()))
                                                    .build());


            startNewTaskIfNotRunning(runnerData, runnerInput.deploymentPlanExecutionMetadata().sfnToken(), runnerInput);


        } catch (TaskStartFailedException e) {
            logger.error("TaskStartFailed", e);
            stepFunctionFacade.sendError(runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                         e.getTaskStatus().stopReason(),
                                         e.getTaskStatus().stopCode().map(Enum::name).orElse("unknown stop code"));
        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument", e);
            stepFunctionFacade.sendError(runnerInput.deploymentPlanExecutionMetadata().sfnToken(),
                                         e.getMessage(),
                                         "RunnerConfigError");
        }
    }

    private void startNewTaskIfNotRunning(RunnerData runnerData, String sfnToken, RunnerInput runnerInput) {

        runnerData.getTaskId()
                  .ifPresentOrElse(taskId -> {
                      TaskStatus taskStatus = ecsFacade.getTaskStatus(
                              taskId, runnerData.getCluster());

                      if (taskIdHasChanged(runnerData, runnerInput)) {
                          return;
                      }
                      if (taskStatus.isRunning() && configurationHasChanged(runnerData)) {
                          logger.info("The task configuration has changed, will stop old task");
                          ecsFacade.stopTask(taskId, runnerData.getCluster());
                          startTask(runnerData, sfnToken, runnerInput);
                      } else if (taskStatus.isStoppingOrStopped()) {
                          startTask(runnerData, sfnToken, runnerInput);
                      }
                  }, () -> startTask(runnerData, sfnToken, runnerInput));


    }

    private boolean taskIdHasChanged(RunnerData runnerData, RunnerInput runnerInput) {
        Optional<String> currentTaskId = stackDataDynamoFacade.getRunnerData(runnerInput.deployOriginData()
                                                                                        .getStackName(),
                                                                             runnerInput.properties()
                                                                                        .runner(), true)
                                                              .getTaskId();

        return !runnerData.getTaskId().equals(currentTaskId);
    }

    private boolean configurationHasChanged(RunnerData currentRunnerData) {
        return !currentRunnerData.getTaskConfigurationHashCode()
                                 .equals(currentRunnerData.getTaskConfiguration().hashCode());
    }

    private String createInput(RunnerInput runnerInput, TaskConfiguration runnerConfig) {
        ObjectNode mutableInput = objectMapper.valueToTree(runnerInput);
        mutableInput.put("taskConfigHashCode", runnerConfig.hashCode());
        return mutableInput.toString();
    }

    private RunnerData updateRunnerData(RunnerData runnerData, String taskId) {
        RunnerData newRunnerData = runnerData.toBuilder()
                                             .taskId(taskId)
                                             .taskConfigurationHashCode(runnerData.getTaskConfiguration().hashCode())
                                             .started(false)
                                             .build();
        stackDataDynamoFacade.saveRunnerData(newRunnerData);
        return newRunnerData;
    }


    private TaskStatus waitForStart(String taskId, String cluster, RunnerInput runnerInput) {
        TaskStatus taskStatus = ecsFacade.getTaskStatus(taskId, cluster);
        if (taskStatus.isDead()) {
            // sometimes when you describe right after the task is started it will return null, in that case, try again
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
        for (int i = 0; i < 300; i++) {
            waitFor(2);
            RunnerData runnerData1 = stackDataDynamoFacade.getRunnerData(runnerInput.deployOriginData().getStackName(),
                                                                         runnerInput.properties().runner());


            if (!runnerData1.getTaskId().map(s -> s.equals(taskId)).orElse(false)) {
                logger.error("TaskId changed in states table. Will stop task");
                ecsFacade.stopTask(taskId, cluster);
                return;
            }

            if (runnerData1.isStarted()) {
                return;
            }

            TaskStatus taskStatus1 = ecsFacade.getTaskStatus(taskId, cluster);
            if (taskStatus1.isStoppingOrStopped()) {
                throw new TaskStartFailedException(taskStatus1);
            }
        }

        ecsFacade.stopTask(taskId, cluster);


    }

    private void waitFor(int time) {
        try {
            TimeUnit.SECONDS.sleep(time);
        } catch (InterruptedException e) {
            logger.error("Sleep in polling was interrupted", e);
        }
    }

    private void startTask(RunnerData runnerData, String sfnToken, RunnerInput runnerInput) {
        logger.info("Starting new task");

        String taskArn = ecsFacade.startTask(runnerData.getTaskConfiguration(),
                                             runnerData.getStackName(),
                                             runnerData.getRunnerName(),
                                             sfnToken);
        RunnerData updateRunnerData = updateRunnerData(runnerData, taskArn);

        TaskStatus taskStatus = waitForStart(taskArn, updateRunnerData.getTaskConfiguration().cluster(), runnerInput);
        if (!taskStatus.isRunning()) {
            logger.error("Task failed to start");
            throw new TaskStartFailedException(taskStatus);
        }
    }

}
