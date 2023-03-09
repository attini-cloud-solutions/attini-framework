/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.CloudFormationClientFactory;
import attini.action.CloudFormationClientFactory.GetCloudFormationClientRequest;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.builders.TestBuilders;
import attini.action.domain.DesiredState;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;


@ExtendWith(MockitoExtension.class)
class CfnStackFacadeTest {

    private static final String NOTIFICATION_ARN = "my.notification.arn";


    @Mock
    CloudFormationClient cloudFormationClient;

    @Mock
    CloudFormationClientFactory cloudFormationClientFactory;

    @Mock
    EnvironmentVariables environmentVariables;

    CfnStackFacade cfnStackFacade;

    @BeforeEach
    void setUp() {
        cfnStackFacade = new CfnStackFacade(cloudFormationClientFactory, environmentVariables);
        when(cloudFormationClientFactory.getClient(any(GetCloudFormationClientRequest.class)))
                .thenReturn(cloudFormationClient);

    }

    @Test
    void shouldCreateStack() {
        when(environmentVariables.getSnsNotificationArn()).thenReturn(NOTIFICATION_ARN);
        when(cloudFormationClient.createStack(any(CreateStackRequest.class)))
                .thenReturn(CreateStackResponse.builder()
                                               .stackId("a-stack-id")
                                               .build());

        StackConfiguration stackConfiguration =
                TestBuilders.aStackConfig()
                            .setTemplate("/a/path/config.json")
                            .setParameters(List.of(Parameter.builder()
                                                        .parameterKey("Ram")
                                                        .parameterValue("256")
                                                        .build()))
                            .build();

        StackData stackData = TestBuilders.aStackData()
                                          .setStackConfiguration(stackConfiguration)
                                          .build();


        ArgumentCaptor<CreateStackRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateStackRequest.class);

        cfnStackFacade.createCfnStack(stackData);
        verify(cloudFormationClient).createStack(createRequestCaptor.capture());
        CreateStackRequest request = createRequestCaptor.getValue();
        assertEquals(1, request.parameters().size());
        assertEquals("Ram", request.parameters().get(0).parameterKey());
        assertEquals("256", request.parameters().get(0).parameterValue());
        assertEquals(stackData.getStackConfiguration().getStackName(), request.stackName());
        assertEquals(stackData.getStackConfiguration().getTemplate(), request.templateURL());
        assertEquals(List.of(NOTIFICATION_ARN), request.notificationARNs());
    }

    @Test
    void shouldUpdateStack() {
        when(environmentVariables.getSnsNotificationArn()).thenReturn(NOTIFICATION_ARN);

        when(cloudFormationClient.updateStack(any(UpdateStackRequest.class)))
                .thenReturn(UpdateStackResponse.builder()
                                               .stackId("a-stack-id")
                                               .build());
        StackConfiguration stackConfiguration =
                TestBuilders.aStackConfig()
                            .setTemplate("/a/path/config.json")
                            .setParameters(List.of(Parameter.builder()
                                                        .parameterKey("Ram")
                                                        .parameterValue("256")
                                                        .build()))
                            .build();

        StackData stackData = TestBuilders.aStackData()
                                          .setStackConfiguration(stackConfiguration)
                                          .build();


        ArgumentCaptor<UpdateStackRequest> updateRequestCaptor = ArgumentCaptor.forClass(UpdateStackRequest.class);

        cfnStackFacade.updateCfnStack(stackData);
        verify(cloudFormationClient).updateStack(updateRequestCaptor.capture());
        UpdateStackRequest request = updateRequestCaptor.getValue();
        assertEquals(1, request.parameters().size());
        assertEquals("Ram", request.parameters().get(0).parameterKey());
        assertEquals("256", request.parameters().get(0).parameterValue());
        assertEquals(stackData.getStackConfiguration().getStackName(), request.stackName());
        assertEquals(stackData.getStackConfiguration().getTemplate(), request.templateURL());
        assertEquals(List.of(NOTIFICATION_ARN), request.notificationARNs());
    }

    @Test
    void shouldDeleteStack() {
        StackData stackData =
                TestBuilders.aStackData()
                            .setStackConfiguration(TestBuilders.aStackConfig().setTemplate("/a/path/config.json")
                                                               .build())
                            .build();
        cfnStackFacade.deleteStack(stackData);
        verify(cloudFormationClient).deleteStack(any(DeleteStackRequest.class));
    }

    @Test
    void deleteStack() {
        StackData stackData =
                TestBuilders.aStackData()
                            .setStackConfiguration(TestBuilders.aStackConfig().setDesiredState(DesiredState.DELETED)
                                                               .build()).build();

        cfnStackFacade.deleteStack(stackData);

        verify(cloudFormationClient).deleteStack(any(DeleteStackRequest.class));

    }

    @Test
    void deleteStack_alreadyDeleted_DontThrowException() {
        StackData stackData =
                TestBuilders.aStackData()
                            .setStackConfiguration(TestBuilders.aStackConfig().setDesiredState(DesiredState.DELETED)
                                                               .build()).build();

        when(cloudFormationClient.deleteStack(any(DeleteStackRequest.class)))
                .thenThrow(CloudFormationException.builder()
                                                  .awsErrorDetails(
                                                          AwsErrorDetails.builder()
                                                                         .errorCode("ValidationError")
                                                                         .build())
                                                  .build());

         cfnStackFacade.deleteStack(stackData);
        verify(cloudFormationClient).deleteStack(any(DeleteStackRequest.class));


    }

}
