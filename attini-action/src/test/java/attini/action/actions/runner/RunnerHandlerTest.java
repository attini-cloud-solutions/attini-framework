package attini.action.actions.runner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.actions.runner.input.RunnerInput;
import attini.action.facades.stackdata.StackDataDynamoFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import software.amazon.awssdk.services.sqs.SqsClient;

@ExtendWith(MockitoExtension.class)
class RunnerHandlerTest {


    @Mock
    private SqsClient sqsClient;
    @Mock
    private EcsFacade ecsFacade;
    @Mock
    private StackDataDynamoFacade stackDataDynamoFacade;
    @Mock
    private StepFunctionFacade stepFunctionFacade;

    @Mock
    private Ec2Facade ec2Facade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    RunnerHandler runnerHandler;

    @BeforeEach
    void setUp() {
        runnerHandler = new RunnerHandler(sqsClient,
                                          ecsFacade,
                                          stackDataDynamoFacade,
                                          stepFunctionFacade,
                                          objectMapper, ec2Facade);
    }

    @Test
    void shouldStartNewTaskBecauseNoneIsRunning() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .taskId("a-task-id")
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .build();


        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(), runnerData.getCluster())).thenReturn(
                TaskStatus.deadTask());
        when(ecsFacade.startTask(any(), anyString())).thenReturn("my-new-id");
        when(ecsFacade.getTaskStatus("my-new-id", runnerData.getCluster())).thenReturn(new TaskStatus(
                "RUNNING",
                "RUNNING",
                null,
                null));

        runnerHandler.handle(runnerInput);
        verify(ecsFacade).startTask(runnerData, runnerInput.deploymentPlanExecutionMetadata().sfnToken());
    }

    @Test
    void shouldStartNewEc2InstanceAndEcsTask() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        Ec2 ec2 = Ec2.builder()
                     .ec2Config(Ec2Config.builder()
                                         .instanceProfile("someInstanceProfile")
                                         .instanceType("m5.large")
                                         .ecsClientLogGroup("someLogGroup")
                                         .build())
                     .build();
        String taskId = "a-task-id";
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .ec2(ec2)
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .build();


        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(), runnerData.getCluster())).thenReturn(
                TaskStatus.deadTask());
        when(ecsFacade.startTask(any(), anyString())).thenReturn(taskId);
        when(ecsFacade.getTaskStatus(taskId, runnerData.getCluster())).thenReturn(new TaskStatus(
                "RUNNING",
                "RUNNING",
                null,
                null));
        String ec2InstanceId = "instanceId12322";

        when(ec2Facade.startInstance(ec2, runnerData)).thenReturn(ec2InstanceId);

        runnerHandler.handle(runnerInput);
        verify(ec2Facade).startInstance(ec2, runnerData);
        verify(ecsFacade).startTask(runnerData.toBuilder()
                                              .ec2(ec2.toBuilder()
                                                      .configHashCode(ec2.getEc2Config().hashCode())
                                                      .latestEc2InstanceId(ec2InstanceId)
                                                      .build())
                                              .build(), runnerInput.deploymentPlanExecutionMetadata().sfnToken());

    }

    @Test
    void shouldTerminateEc2InstanceIfEcsTaskFails_sync() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        Ec2 ec2 = Ec2.builder()
                     .ec2Config(Ec2Config.builder()
                                         .instanceProfile("someInstanceProfile")
                                         .instanceType("m5.large")
                                         .ecsClientLogGroup("someLogGroup")
                                         .build())
                     .build();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .ec2(ec2)
                                                     .build();


        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(), runnerData.getCluster())).thenReturn(
                TaskStatus.deadTask());
        when(ecsFacade.startTask(any(), anyString())).thenThrow(new TaskStartFailedSyncException("Failed!"));
        String ec2InstanceId = "instanceId12322";

        when(ec2Facade.startInstance(ec2, runnerData)).thenReturn(ec2InstanceId);

        runnerHandler.handle(runnerInput);
        verify(ec2Facade).startInstance(ec2, runnerData);
        verify(ecsFacade).startTask(runnerData.toBuilder()
                                              .ec2(ec2.toBuilder()
                                                      .configHashCode(ec2.getEc2Config().hashCode())
                                                      .latestEc2InstanceId(ec2InstanceId)
                                                      .build())
                                              .build(), runnerInput.deploymentPlanExecutionMetadata().sfnToken());
        verify(ec2Facade).terminateInstance(ec2InstanceId);

    }

    @Test
    void shouldTerminateEc2InstanceIfEcsTaskFails_async() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        Ec2 ec2 = Ec2.builder()
                     .ec2Config(Ec2Config.builder()
                                         .instanceProfile("someInstanceProfile")
                                         .instanceType("m5.large")
                                         .ecsClientLogGroup("someLogGroup")
                                         .build())
                     .build();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .ec2(ec2)
                                                     .build();


        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(), runnerData.getCluster())).thenReturn(
                TaskStatus.deadTask());
        when(ecsFacade.startTask(any(), anyString())).thenReturn("my-new-id");
        when(ecsFacade.getTaskStatus("my-new-id", runnerData.getCluster())).thenReturn(TaskStatus.deadTask());
        String ec2InstanceId = "instanceId12322";

        when(ec2Facade.startInstance(ec2, runnerData)).thenReturn(ec2InstanceId);

        runnerHandler.handle(runnerInput);
        verify(ec2Facade).startInstance(ec2, runnerData);
        verify(ecsFacade).startTask(runnerData.toBuilder()
                                              .ec2(ec2.toBuilder()
                                                      .configHashCode(ec2.getEc2Config().hashCode())
                                                      .latestEc2InstanceId(ec2InstanceId)
                                                      .build())
                                              .build(), runnerInput.deploymentPlanExecutionMetadata().sfnToken());
        verify(ec2Facade).terminateInstance(ec2InstanceId);

    }

    @Test
    void shouldRestartEc2IfConfigChange() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        String instanceId = "someId";
        Ec2 ec2 = Ec2.builder()
                     .ec2Config(Ec2Config.builder()
                                         .instanceProfile("someInstanceProfile")
                                         .instanceType("m5.large")
                                         .ecsClientLogGroup("someLogGroup")
                                         .build())
                     .latestEc2InstanceId(instanceId)
                     .configHashCode(1)
                     .build();

        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .ec2(ec2)
                                                     .build();
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.startTask(any(), anyString())).thenReturn("my-new-id");
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(), runnerData.getCluster())).thenReturn(
                TaskStatus.deadTask());
        when(ecsFacade.getTaskStatus("my-new-id", runnerData.getCluster())).thenReturn(new TaskStatus(
                "RUNNING",
                "RUNNING",
                null,
                null));
        when(ec2Facade.instanceIsRunning(instanceId)).thenReturn(true);
        when(ec2Facade.startInstance(ec2, runnerData)).thenReturn("a-instance-Id");

        runnerHandler.handle(runnerInput);

        verify(ec2Facade).terminateInstance(instanceId);
        verify(ec2Facade).startInstance(ec2, runnerData);

    }

    @Test
    void shouldNotStartNewTaskBecauseItIsRunning() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .taskId("a-task-id")
                                                     .build();


        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(),
                                     runnerData.getCluster())).thenReturn(new TaskStatus("RUNNING",
                                                                                         "RUNNING",
                                                                                         null,
                                                                                         null));

        runnerHandler.handle(runnerInput);
        verify(ecsFacade, never()).startTask(runnerData, runnerInput.deploymentPlanExecutionMetadata().sfnToken());
    }

    @Test
    void shouldStartNewTaskAndStopOldBecauseConfigChange() {

        RunnerInput runnerInput = InputBuilder.aRunnerInput();

        TaskConfiguration taskConfiguration = RunnerDataTestBuilder.aTaskConfiguration()
                                                                   .container("my-new-container")
                                                                   .build();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .taskId("a-new-task-id")
                                                     .startedByExecutionArn(runnerInput.deploymentPlanExecutionMetadata()
                                                                                       .executionArn())
                                                     .taskConfiguration(taskConfiguration)
                                                     .started(true)
                                                     .build();


        when(ecsFacade.startTask(any(), anyString())).thenReturn("a-new-task-id");
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString())).thenReturn(
                runnerData);
        when(stackDataDynamoFacade.getRunnerData(anyString(), anyString(), anyBoolean())).thenReturn(
                runnerData);
        when(ecsFacade.getTaskStatus(runnerData.getTaskId().get(),
                                     runnerData.getCluster())).thenReturn(new TaskStatus("RUNNING",
                                                                                         "RUNNING",
                                                                                         null,
                                                                                         null));

        runnerHandler.handle(runnerInput);
        verify(ecsFacade).stopTask(runnerData.getTaskId().get(), runnerData.getCluster());
        verify(ecsFacade).startTask(runnerData, runnerInput.deploymentPlanExecutionMetadata().sfnToken());
    }


}
