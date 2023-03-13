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

import attini.step.guard.cloudformation.CfnEventHandler;
import attini.step.guard.cloudformation.CfnEventType;
import attini.step.guard.cloudformation.CfnOutputCreator;
import attini.step.guard.cloudformation.CfnSnsEventTypeResolver;
import attini.step.guard.cloudformation.CloudFormationManualTriggerEvent;
import attini.step.guard.cloudformation.CloudFormationSnsEventImpl;
import attini.step.guard.cloudformation.StackErrorResolver;
import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.StackData;
import attini.step.guard.stackdata.StackDataFacade;

@ExtendWith(MockitoExtension.class)
class CfnEventHandlerTest {

    @Mock
    StepFunctionFacade stepFunctionFacade;
    @Mock
    StackDataFacade stackDataFacade;
    @Mock
    CfnOutputCreator cfnOutputCreator;
    @Mock
    StackErrorResolver stackErrorResolver;
    @Mock
    DeployDataFacade deployDataFacade;

    @Mock
    CfnSnsEventTypeResolver cfnSnsEventTypeResolver;

    CfnEventHandler cfnEventHandler;

    @BeforeEach
    void setUp() {
        cfnEventHandler = new CfnEventHandler(stepFunctionFacade,
                                              stackDataFacade,
                                              cfnOutputCreator,
                                              stackErrorResolver,
                                              deployDataFacade,
                                              cfnSnsEventTypeResolver);
    }

    @Test
    void respondToCfnEvent_manual() {
        String sfnToken = "a-token";
        CloudFormationManualTriggerEvent input = StepGuardInputBuilder.aManualTrigger()
                                                                      .setSfnResponseToken(sfnToken)
                                                                      .build();
        String cfnOutput = "my-output";
        when(cfnOutputCreator.createCfnOutput(input)).thenReturn(cfnOutput);
        cfnEventHandler.respondToManualCfnEvent(input);
        verify(cfnOutputCreator).createCfnOutput(input);
        verify(stepFunctionFacade).sendTaskSuccess(sfnToken, cfnOutput);
        verify(stackDataFacade, never()).getStackData(input);
    }

    @Test
    void respondToCfnEvent_manual_deleteStack() {
        String sfnToken = "a-token";
        CloudFormationManualTriggerEvent input = StepGuardInputBuilder.aManualTrigger()
                                                                      .setSfnResponseToken(sfnToken)
                                                                      .setResourceStatus("DELETE_COMPLETE")
                                                                      .setDesiredState(DesiredState.DELETED)
                                                                      .build();
        String cfnOutput = "my-output";
        when(cfnOutputCreator.createCfnOutput(input)).thenReturn(cfnOutput);
        cfnEventHandler.respondToManualCfnEvent(input);
        verify(cfnOutputCreator).createCfnOutput(input);
        verify(stepFunctionFacade).sendTaskSuccess(sfnToken, cfnOutput);
        verify(stackDataFacade, never()).getStackData(input);
    }

    @Test
    void respondToCfnEvent_manual_failedStatus() {

        String cfnOutput = "my-output";
        String sfnToken = "a-token";

        CloudFormationManualTriggerEvent input = StepGuardInputBuilder.aManualTrigger()
                                                                      .setStackId("my-id")
                                                                      .setSfnResponseToken(sfnToken)
                                                                      .setResourceStatus("UPDATE_ROLLBACK_FAILED")
                                                                      .build();


        StackError error = StackError.defaultError();
        when(stackErrorResolver.resolveError(input)).thenReturn(error);
        cfnEventHandler.respondToManualCfnEvent(input);
        verify(cfnOutputCreator, never()).createCfnOutput(any(), any());
        verify(stepFunctionFacade, never()).sendTaskSuccess(sfnToken, cfnOutput);
        verify(stepFunctionFacade).sendTaskFailure(sfnToken,
                                                   error.getMessage(),
                                                   error.getErrorStatus());
        verify(stackDataFacade, never()).getStackData(input);
    }

    @Test
    void respondToCfnEvent_sns() {
        CloudFormationSnsEventImpl input = StepGuardInputBuilder.aSnsTrigger().build();
        String cfnOutput = "my-output";
        String sfnToken = "a-token";
        StackData stackData = StackDataTestBuilder.aStackData().setSfnToken(sfnToken).build();
        when(cfnSnsEventTypeResolver.resolve(input)).thenReturn(CfnEventType.STACK_UPDATED);

        when(cfnOutputCreator.createCfnOutput(stackData, input)).thenReturn(cfnOutput);
        when(stackDataFacade.getStackData(input)).thenReturn(stackData);
        cfnEventHandler.respondToCloudFormationSnsEvent(input);


        verify(cfnOutputCreator).createCfnOutput(stackData, input);
        verify(stepFunctionFacade).sendTaskSuccess(sfnToken, cfnOutput);
        verify(stackDataFacade).getStackData(input);
    }

    @Test
    void respondToCfnEvent_sns_deleteStack() {
        CloudFormationSnsEventImpl input =
                StepGuardInputBuilder.aSnsTrigger()
                                     .setResourceStatus("DELETE_COMPLETE")
                                     .build();
        String cfnOutput = "my-output";
        String sfnToken = "a-token";
        StackData stackData = StackDataTestBuilder.aStackData()
                                                  .setDesiredState(DesiredState.DELETED)
                                                  .build();

        when(cfnSnsEventTypeResolver.resolve(input)).thenReturn(CfnEventType.STACK_DELETED);
        when(cfnOutputCreator.createCfnOutput(stackData, input)).thenReturn(cfnOutput);
        when(stackDataFacade.getStackData(input)).thenReturn(stackData);

        cfnEventHandler.respondToCloudFormationSnsEvent(input);


        verify(cfnOutputCreator).createCfnOutput(stackData, input);
        verify(stepFunctionFacade).sendTaskSuccess(sfnToken, cfnOutput);
        verify(stackDataFacade).getStackData(input);
    }

    @Test
    void respondToCfnEvent_notCfnEvent_doNothing() {
        CloudFormationSnsEventImpl input = StepGuardInputBuilder.aSnsTrigger()
                                                                .build();

        when(cfnSnsEventTypeResolver.resolve(input)).thenReturn(CfnEventType.RESOURCE_UPDATE);
        cfnEventHandler.respondToCloudFormationSnsEvent(input);
        verify(cfnOutputCreator, never()).createCfnOutput(any(), any());
        verify(stepFunctionFacade, never()).sendTaskSuccess(anyString(), anyString());
        verify(stackDataFacade, never()).getStackData(any());
    }

    @Test
    void respondToCfnEvent_sns_failedStatus() {
        CloudFormationSnsEventImpl input = StepGuardInputBuilder.aSnsTrigger()
                                                                .setStackId("my-id")
                                                                .setResourceStatus("UPDATE_ROLLBACK_FAILED")
                                                                .build();
        String cfnOutput = "my-output";
        String sfnToken = "a-token";

        StackData stackData = StackDataTestBuilder.aStackData()
                                                  .setSfnToken(sfnToken)
                                                  .setStackId("my-id")
                                                  .build();

        when(cfnSnsEventTypeResolver.resolve(input)).thenReturn(CfnEventType.STACK_FAILED);
        when(stackDataFacade.getStackData(input)).thenReturn(stackData);
        StackError error = StackError.defaultError();
        when(stackErrorResolver.resolveError(input)).thenReturn(error);
        cfnEventHandler.respondToCloudFormationSnsEvent(input);
        verify(cfnOutputCreator, never()).createCfnOutput(any(), any());
        verify(stepFunctionFacade, never()).sendTaskSuccess(sfnToken, cfnOutput);
        verify(stepFunctionFacade).sendTaskFailure(sfnToken,
                                                   error.getMessage(),
                                                   error.getErrorStatus());
        verify(stackDataFacade).getStackData(input);
    }

}
