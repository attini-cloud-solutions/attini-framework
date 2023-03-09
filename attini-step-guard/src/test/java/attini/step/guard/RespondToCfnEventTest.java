package attini.step.guard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.StackData;
import attini.step.guard.stackdata.StackDataFacade;
import software.amazon.awssdk.services.sfn.SfnClient;

@ExtendWith(MockitoExtension.class)
class RespondToCfnEventTest {

    @Mock
    SfnResponseSender sfnResponseSender;
    @Mock
    SfnClient sfnClient;
    @Mock
    StackDataFacade stackDataFacade;
    @Mock
    CfnOutputCreator cfnOutputCreator;
    @Mock
    StackErrorResolver stackErrorResolver;
    @Mock
    PublishEventService publishEventService;
    @Mock
    DeployDataFacade deployDataFacade;

    RespondToCfnEvent respondToCfnEvent;

    @BeforeEach
    void setUp() {
        respondToCfnEvent = new RespondToCfnEvent(sfnResponseSender,
                                                  sfnClient,
                                                  stackDataFacade,
                                                  cfnOutputCreator,
                                                  stackErrorResolver,
                                                  publishEventService,
                                                  deployDataFacade);
    }

    @Test
    void respondToCfnEvent_manual() {
        CloudFormationManualTriggerEvent input = StepGuardInputBuilder.aManualTrigger().build();
        String cfnOutput = "my-output";
        when(cfnOutputCreator.createCfnOutput(input)).thenReturn(cfnOutput);
        respondToCfnEvent.respondToManualCfnEvent(input);
        verify(cfnOutputCreator).createCfnOutput(input);
        verify(publishEventService).postStepCompleted(input);
        verify(sfnResponseSender).sendTaskSuccess(input.getSfnResponseToken().get(), cfnOutput);
        verify(stackDataFacade, never()).getStackData(input);
    }

    @Test
    void respondToCfnEvent_manual_deleteStack() {
        CloudFormationManualTriggerEvent input = StepGuardInputBuilder.aManualTrigger().setResourceStatus("DELETE_COMPLETE").setDesiredState(DesiredState.DELETED).build();
        String cfnOutput = "my-output";
        when(cfnOutputCreator.createCfnOutput(input)).thenReturn(cfnOutput);
        respondToCfnEvent.respondToManualCfnEvent(input);
        verify(cfnOutputCreator).createCfnOutput(input);
        verify(publishEventService).postStepCompleted(input);
        verify(sfnResponseSender).sendTaskSuccess(input.getSfnResponseToken().get(), cfnOutput);
        verify(stackDataFacade, never()).getStackData(input);
    }

    @Test
    void respondToCfnEvent_sns() {
        CloudFormationSnsEvent input = StepGuardInputBuilder.aSnsTrigger().setStackId("my-id").build();
        String cfnOutput = "my-output";
        StackData stackData = StackDataTestBuilder.aStackData().setStackId("my-id").build();
        when(cfnOutputCreator.createCfnOutput(stackData, input)).thenReturn(cfnOutput);
        when(stackDataFacade.getStackData(input)).thenReturn(stackData);
        respondToCfnEvent.respondToCloudFormationSnsEvent(input);
        verify(cfnOutputCreator).createCfnOutput(stackData, input);
        verify(publishEventService).postStepCompleted(stackData, input.getStackName());
        verify(sfnResponseSender).sendTaskSuccess(stackData.getSfnToken().get(), cfnOutput);
        verify(stackDataFacade).getStackData(input);
    }

    @Test
    void respondToCfnEvent_sns_deleteStack() {
        CloudFormationSnsEvent input = StepGuardInputBuilder.aSnsTrigger().setResourceStatus("DELETE_COMPLETE").setStackId("my-id").build();
        String cfnOutput = "my-output";
        StackData stackData = StackDataTestBuilder.aStackData()
                                                  .setStackId("my-id")
                                                  .setDesiredState(DesiredState.DELETED)
                                                  .build();
        when(cfnOutputCreator.createCfnOutput(stackData, input)).thenReturn(cfnOutput);
        when(stackDataFacade.getStackData(input)).thenReturn(stackData);
        respondToCfnEvent.respondToCloudFormationSnsEvent(input);
        verify(cfnOutputCreator).createCfnOutput(stackData, input);
        verify(publishEventService).postStepCompleted(stackData, input.getStackName());
        verify(sfnResponseSender).sendTaskSuccess(stackData.getSfnToken().get(), cfnOutput);
        verify(stackDataFacade).getStackData(input);
    }

    @Test
    void respondToCfnEvent_notCfnEvent_doNothing() {
        CloudFormationSnsEvent input = StepGuardInputBuilder.aSnsTrigger().setResourceType("something-else").build();
        respondToCfnEvent.respondToCloudFormationSnsEvent(input);
        verify(cfnOutputCreator, never()).createCfnOutput(any(), any());
        verify(publishEventService, never()).postStepCompleted(any(), anyString());
        verify(sfnResponseSender, never()).sendTaskSuccess(anyString(), anyString());
        verify(stackDataFacade, never()).getStackData(any());
    }

    @Test
    void respondToCfnEvent_manual_failedStatus() {
        CloudFormationManualTriggerEvent input = StepGuardInputBuilder.aManualTrigger()
                                                                      .setStackId("my-id")
                                                                      .setResourceStatus("UPDATE_ROLLBACK_FAILED")
                                                                      .build();
        String cfnOutput = "my-output";
        StackError error = StackError.defaultError();
        when(stackErrorResolver.resolveError(input)).thenReturn(error);
        respondToCfnEvent.respondToManualCfnEvent(input);
        verify(cfnOutputCreator, never()).createCfnOutput(any(), any());
        verify(publishEventService).postStepCompleted(input, error);
        verify(sfnResponseSender, never()).sendTaskSuccess(input.getSfnResponseToken().get(), cfnOutput);
        verify(sfnResponseSender).sendTaskFailure(input.getSfnResponseToken().get(),
                                                  error.getMessage(),
                                                  error.getErrorStatus());
        verify(stackDataFacade, never()).getStackData(input);
    }

    @Test
    void respondToCfnEvent_sns_failedStatus() {
        CloudFormationSnsEvent input = StepGuardInputBuilder.aSnsTrigger()
                                                                 .setStackId("my-id")
                                                                 .setResourceStatus("UPDATE_ROLLBACK_FAILED")
                                                                 .build();
        String cfnOutput = "my-output";
        StackData stackData = StackDataTestBuilder.aStackData().setStackId("my-id").build();
        when(stackDataFacade.getStackData(input)).thenReturn(stackData);
        StackError error = StackError.defaultError();
        when(stackErrorResolver.resolveError(input)).thenReturn(error);
        respondToCfnEvent.respondToCloudFormationSnsEvent(input);
        verify(cfnOutputCreator, never()).createCfnOutput(any(), any());
        verify(publishEventService).postStepCompleted(stackData, input.getStackName(), error);
        verify(sfnResponseSender, never()).sendTaskSuccess(stackData.getSfnToken().get(), cfnOutput);
        verify(sfnResponseSender).sendTaskFailure(stackData.getSfnToken().get(),
                                                  error.getMessage(),
                                                  error.getErrorStatus());
        verify(stackDataFacade).getStackData(input);
    }

}
