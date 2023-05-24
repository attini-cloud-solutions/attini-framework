package attini.action.actions.deploycloudformation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.builders.TestBuilders;
import attini.action.domain.DesiredState;
import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.facades.stepguard.StepGuardFacade;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

@ExtendWith(MockitoExtension.class)
class DeployCfnCrossRegionServiceTest {

    @Mock
    private CfnStackFacade cfnStackFacade;
    @Mock
    private StepGuardFacade stepGuardFacade;
    @Mock
    private ResourceStateFacade resourceStateFacade;
    @Mock
    private StepFunctionFacade stepFunctionFacade;
    @Mock
    private CfnErrorHandler cfnErrorHandler;

    DeployCfnCrossRegionService deployCfnCrossRegionService;

    @BeforeEach
    void setUp() {
        deployCfnCrossRegionService = new DeployCfnCrossRegionService(cfnStackFacade,
                                                                      stepGuardFacade,
                                                                      resourceStateFacade,
                                                                      stepFunctionFacade,
                                                                      cfnErrorHandler);
    }

    @Test
    public void shouldUpdateStack() {
        StackData stackData = TestBuilders.aStackData().build();

        when(resourceStateFacade.getStacksSfnExecutionArn(stackData.getStackConfiguration())).thenReturn(Optional.of(
                SfnExecutionArn.create("")));

        when(cfnStackFacade.getStackStatus(stackData)).thenReturn(new StackStatus("22222",
                                                                                  StackStatus.StackState.COMPLETE,
                                                                                  "UPDATE_COMPLETE",
                                                                                  ""));


        deployCfnCrossRegionService.deployWithPolling(stackData);

        verify(cfnStackFacade).updateStackCrossRegion(stackData);

    }

    @Test
    public void shouldCreateStack() {
        StackData stackData = TestBuilders.aStackData().build();

        when(cfnStackFacade.getStackStatus(stackData)).thenThrow(CloudFormationException.builder().awsErrorDetails(
                AwsErrorDetails.builder().errorCode("Unimportant").errorMessage("does not exist").build()).build());


        deployCfnCrossRegionService.deployWithPolling(stackData);

        verify(cfnStackFacade).createStackCrossRegion(stackData);

    }

    @Test
    public void shouldDeleteStack() {

        StackData stackData = TestBuilders.aStackData()
                                          .setStackConfiguration(TestBuilders.aStackConfig()
                                                                             .setDesiredState(DesiredState.DELETED)
                                                                             .build())
                                          .build();

        when(resourceStateFacade.getStacksSfnExecutionArn(stackData.getStackConfiguration())).thenReturn(Optional.of(
                SfnExecutionArn.create("")));

        when(cfnStackFacade.getStackStatus(stackData)).thenReturn(new StackStatus("22222",
                                                                                  StackStatus.StackState.COMPLETE,
                                                                                  "UPDATE_COMPLETE",
                                                                                  ""));


        deployCfnCrossRegionService.deployWithPolling(stackData);

        verify(cfnStackFacade).deleteStack(stackData);
    }

    @Test
    public void shouldSkipDeleteBecauseStackIsDeleted() {
        StackData stackData = TestBuilders.aStackData()
                                          .setStackConfiguration(TestBuilders.aStackConfig()
                                                                             .setDesiredState(DesiredState.DELETED)
                                                                             .build())
                                          .build();
        when(cfnStackFacade.getStackStatus(stackData)).thenThrow(CloudFormationException.builder().awsErrorDetails(
                AwsErrorDetails.builder().errorCode("Unimportant").errorMessage("does not exist").build()).build());


        deployCfnCrossRegionService.deployWithPolling(stackData);

        verify(cfnStackFacade,never()).deleteStack(stackData);

    }

    @Test
    public void shouldRetry() {
        SfnExecutionArn sfnExecutionArn = SfnExecutionArn.create(
                "arn:aws:states:eu-west-1:655047308345:execution:PipelineAttiniDeploymentPlanSfn-JrVqujOxtZcG:03e10a98-86fe-466e-8f05");
        StackData stackData = TestBuilders.aStackData().setDeploymentPlanExecutionMetadata(TestBuilders.aMetaData(sfnExecutionArn)).build();


        when(cfnStackFacade.getStackStatus(stackData)).thenReturn(new StackStatus(sfnExecutionArn.extractExecutionId()+"12323232",
                                                                                  StackStatus.StackState.UPDATE_IN_PROGRESS,
                                                                                  "UPDATE_IN_PROGRESS",
                                                                                  ""));


        deployCfnCrossRegionService.deployWithPolling(stackData);

        verify(cfnStackFacade, never()).updateStackCrossRegion(stackData);
    }

    @Test
    public void shouldCompleteExecution() {
        SfnExecutionArn sfnExecutionArn = SfnExecutionArn.create(
                "arn:aws:states:eu-west-1:655047308345:execution:PipelineAttiniDeploymentPlanSfn-JrVqujOxtZcG:03e10a98-86fe-466e-8f05");
        StackData stackData = TestBuilders.aStackData().setDeploymentPlanExecutionMetadata(TestBuilders.aMetaData(sfnExecutionArn)).build();


        when(cfnStackFacade.getStackStatus(stackData)).thenReturn(new StackStatus(sfnExecutionArn.extractExecutionId()+"12323232",
                                                                                  StackStatus.StackState.COMPLETE,
                                                                                  "UPDATE_COMPLETE",
                                                                                  ""));


        deployCfnCrossRegionService.deployWithPolling(stackData);

        verify(cfnStackFacade, never()).updateStackCrossRegion(stackData);
        verify(stepGuardFacade).notifyStepCompleted(any(), anyString(), anyString());
    }
}
