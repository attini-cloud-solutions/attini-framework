/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.builders.TestBuilders;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.action.domain.DesiredState;
import attini.action.facades.stackdata.StackDataFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

@ExtendWith(MockitoExtension.class)
class DeployCfnStackServiceTest {


    @Mock
    CfnStackFacade cfnStackFacade;
    @Mock
    StackDataFacade stackDataFacade;
    @Mock
    StepFunctionFacade stepFunctionFacade;
    @Mock
    CfnErrorHandler cfnErrorHandler;

    DeployCfnService deployCfnStackService;

    @BeforeEach
    void setUp() {
        deployCfnStackService = new DeployCfnService(cfnStackFacade,
                                                     stackDataFacade,
                                                     stepFunctionFacade,
                                                     cfnErrorHandler);

    }


    @Test
    public void deployStack() {
        when(cfnStackFacade.updateCfnStack(any())).thenReturn("233232");
        StackData stackData = TestBuilders.aStackData().build();
        deployCfnStackService.deployStack(stackData);

        verify(stackDataFacade).saveToken(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                          stackData.getStackConfiguration());
        verify(cfnStackFacade).updateCfnStack(stackData);

    }

    @Test
    public void deployStack_shouldDeleteStack() {

        StackData stackData = TestBuilders.aStackData()
                                          .setStackConfiguration(TestBuilders.aStackConfig()
                                                                             .setDesiredState(DesiredState.DELETED)
                                                                             .build())
                                          .build();
        deployCfnStackService.deployStack(stackData);

        verify(stackDataFacade).saveToken(stackData.getDeploymentPlanExecutionMetadata().sfnToken(),
                                          stackData.getStackConfiguration());
        verify(cfnStackFacade).deleteStack(stackData);

    }

    @Test
    public void deployStack_cloudFormationError_ifNoUpdatePerformedNotifyComplete() {
        StackData stackData = TestBuilders.aStackData().build();
        doThrow(CloudFormationException.builder()
                                       .awsErrorDetails(AwsErrorDetails.builder()
                                                                       .errorMessage("No updates are to be performed.")
                                                                       .build()).build())
                .when(cfnStackFacade).updateCfnStack(stackData);

        deployCfnStackService.deployStack(stackData);

        verify(cfnStackFacade).updateCfnStack(stackData);
        verify(cfnErrorHandler).handleNoUpdatesToPerformedState(stackData, "UPDATE_COMPLETE");

    }

    @Test
    public void deployStack_cloudFormationError_ifRollBackCompleteRecreateStack() {
        StackData stackData = TestBuilders.aStackData().build();

        doThrow(CloudFormationException.builder()
                                       .awsErrorDetails(AwsErrorDetails.builder()
                                                                       .errorMessage(
                                                                               "is in ROLLBACK_COMPLETE state and can not be updated.")
                                                                       .build()).build())
                .when(cfnStackFacade).updateCfnStack(stackData);

        deployCfnStackService.deployStack(stackData);



        verify(cfnStackFacade).updateCfnStack(stackData);
        verify(cfnErrorHandler).handleRollbackCompleteState(stackData);

    }

    @Test
    public void deployStack_cloudFormationError_ifStackDontExistCreateIt() {
        StackData stackData = TestBuilders.aStackData().build();
        DeploymentPlanExecutionMetadata metaData = stackData.getDeploymentPlanExecutionMetadata();
        StackConfiguration stackConfig = stackData.getStackConfiguration();
        doThrow(CloudFormationException.builder()
                                       .awsErrorDetails(AwsErrorDetails.builder()
                                                                       .errorCode("SomeError")
                                                                       .errorMessage("does not exist")
                                                                       .build()).build())
                .when(cfnStackFacade).updateCfnStack(stackData);

        when(cfnStackFacade.createCfnStack(stackData)).thenReturn("a-stack-id");

        deployCfnStackService.deployStack(stackData);

        verify(stackDataFacade).saveToken(metaData.sfnToken(), stackConfig);
        verify(cfnStackFacade).updateCfnStack(stackData);
        verify(cfnStackFacade).createCfnStack(stackData);

    }

    @Test()
    public void deployStack_cloudFormationError_ifValidationErrorTrowDeployCfnException() {
        StackData stackData = TestBuilders.aStackData().build();
        AwsServiceException awsServiceException = CloudFormationException.builder()
                                                                         .awsErrorDetails(AwsErrorDetails.builder()
                                                                                                         .errorMessage(
                                                                                                                 "Some validation error")
                                                                                                         .errorCode(
                                                                                                                 "ValidationError")
                                                                                                         .build())
                                                                         .build();
        doThrow(awsServiceException).when(cfnStackFacade)
                                    .updateCfnStack(stackData);
        doThrow(DeployCfnException.class).when(cfnErrorHandler)
                                         .handleValidationError(any(), any());
        assertThrows(DeployCfnException.class, () -> deployCfnStackService.deployStack(stackData));

        verify(cfnStackFacade).updateCfnStack(stackData);
    }
}
