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
    void shouldNotStartNewTaskBecauseItIsRunning() {
        RunnerInput runnerInput = InputBuilder.aRunnerInput();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
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

        TaskConfiguration taskConfiguration = RunnerDataTestBuilder.aTaskConfiguration().container("my-new-container").build();
        RunnerData runnerData = RunnerDataTestBuilder.aRunnerData()
                                                     .taskId("a-new-task-id")
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
