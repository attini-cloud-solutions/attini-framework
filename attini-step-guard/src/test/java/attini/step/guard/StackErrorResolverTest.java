/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.UPDATE_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.UPDATE_IN_PROGRESS;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.step.guard.cloudformation.CloudFormationClientFactory;
import attini.step.guard.cloudformation.CloudFormationSnsEventImpl;
import attini.step.guard.cloudformation.StackError;
import attini.step.guard.cloudformation.StackErrorResolver;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;

@ExtendWith(MockitoExtension.class)
class StackErrorResolverTest {

    private static final String STACK_NAME = "my-init-stack";

    private static final CloudFormationSnsEventImpl CLOUD_FORMATION_SNS_EVENT =
            CloudFormationSnsEventImpl.builder()
                                      .setStackName(STACK_NAME)
                                      .setResourceStatus("UPDATE_FAILED")
                                      .setStackId("a-stack-id")
                                      .setClientRequestToken("test")
                                      .setResourceType("Cloudformation")
                                      .setLogicalResourceId(STACK_NAME)
                                      .build();

    private static final StackError ERROR = StackError.builder()
                                                      .setErrorStatus("UPDATE_FAILED")
                                                      .setResourceId("SomeOtherResource")
                                                      .setMessage("a error message")
                                                      .build();


    @Mock
    CloudFormationClient cloudFormationClient;

    @Mock
    CloudFormationClientFactory cloudFormationClientFactory;

    StackErrorResolver stackErrorResolver;

    @BeforeEach
    void setUp() {
        when(cloudFormationClientFactory.getClient(any())).thenReturn(cloudFormationClient);
        stackErrorResolver = new StackErrorResolver(cloudFormationClientFactory);
    }

    @Test
    void shouldResolveCorrectError() {
        LocalDateTime time = LocalDateTime.of(2021, 2, 2, 14, 0);
        List<StackEvent> stackEvents = List.of(stackEvent(UPDATE_COMPLETE, time, STACK_NAME),
                                               stackEvent(ERROR,
                                                          time.minusSeconds(2)),
                                               stackEvent(UPDATE_IN_PROGRESS,
                                                          time.minusSeconds(4),
                                                          STACK_NAME),
                                               stackEvent(UPDATE_IN_PROGRESS,
                                                          time.minusSeconds(6),
                                                          "SomeOtherResource"),
                                               stackEvent(UPDATE_COMPLETE, time.minusSeconds(8), STACK_NAME),
                                               stackEvent(StackError.builder()
                                                                    .setMessage("some other error")
                                                                    .setErrorStatus("UPDATE_FAILED")
                                                                    .setResourceId("SomeResourceId")
                                                                    .build(),
                                                          time.minusSeconds(10)));

        when(cloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder()
                                                                                .stackName(
                                                                                        CLOUD_FORMATION_SNS_EVENT.getStackId()
                                                                                                                 .orElseGet(
                                                                                                                         CLOUD_FORMATION_SNS_EVENT::getStackName))
                                                                                .build()))
                .thenReturn(DescribeStackEventsResponse.builder().stackEvents(stackEvents).build());

        StackError error = stackErrorResolver.resolveError(CLOUD_FORMATION_SNS_EVENT);
        assertEquals(ERROR, error);
    }

    @Test
    void shouldResolveCorrectError_noPreviousRuns() {
        LocalDateTime time = LocalDateTime.of(2021, 2, 2, 14, 0);
        List<StackEvent> stackEvents = List.of(stackEvent(ERROR,
                                                          time.minusSeconds(2)),
                                               stackEvent(UPDATE_IN_PROGRESS,
                                                          time.minusSeconds(4),
                                                          STACK_NAME),
                                               stackEvent(UPDATE_IN_PROGRESS,
                                                          time.minusSeconds(6),
                                                          "SomeOtherResource"));

        when(cloudFormationClient.describeStackEvents(DescribeStackEventsRequest.builder()
                                                                                .stackName(
                                                                                        CLOUD_FORMATION_SNS_EVENT.getStackId()
                                                                                                                 .orElseGet(
                                                                                                                         CLOUD_FORMATION_SNS_EVENT::getStackName))
                                                                                .build()))
                .thenReturn(DescribeStackEventsResponse.builder().stackEvents(stackEvents).build());

        StackError error = stackErrorResolver.resolveError(CLOUD_FORMATION_SNS_EVENT);
        assertEquals(ERROR, error);
    }

    private static StackEvent stackEvent(StackError error, LocalDateTime timestamp) {
        return StackEvent.builder()
                         .stackName(STACK_NAME)
                         .logicalResourceId(error.getResourceId())
                         .timestamp(timestamp.atZone(ZoneId.systemDefault()).toInstant())
                         .resourceStatus(error.getErrorStatus())
                         .resourceStatusReason(error.getMessage())
                         .resourceType("AWS::CloudFormation::Stack")
                         .build();
    }

    private static StackEvent stackEvent(ResourceStatus status, LocalDateTime timestamp, String logicalId) {
        return StackEvent.builder()
                         .stackName(STACK_NAME)
                         .logicalResourceId(logicalId)
                         .timestamp(timestamp.atZone(ZoneId.systemDefault()).toInstant())
                         .resourceStatus(status)
                         .resourceType("AWS::CloudFormation::Stack")
                         .build();
    }
}
