/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static java.util.Objects.requireNonNull;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.CREATE_FAILED;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.CREATE_IN_PROGRESS;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.DELETE_IN_PROGRESS;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.UPDATE_FAILED;
import static software.amazon.awssdk.services.cloudformation.model.ResourceStatus.UPDATE_IN_PROGRESS;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;


public class StackErrorResolver {

    private static final Logger logger = Logger.getLogger(StackErrorResolver.class);

    private static final EnumSet<ResourceStatus> INIT_STATUSES =
            EnumSet.of(CREATE_IN_PROGRESS, UPDATE_IN_PROGRESS, DELETE_IN_PROGRESS);

    private final CloudFormationClientFactory cloudFormationClientFactory;

    public StackErrorResolver(CloudFormationClientFactory cloudFormationClientFactory) {
        this.cloudFormationClientFactory = requireNonNull(cloudFormationClientFactory, "cloudFormationClientFactory");
    }


    public StackError resolveError(CloudFormationEvent cloudFormationEvent) {

        List<StackEvent> stackEvents = getStackEvents(cloudFormationEvent);

        Optional<Instant> firstTimestamp = getTimeForFirstEvent(stackEvents, cloudFormationEvent.getStackName());

        if (firstTimestamp.isPresent()) {
            logger.info("Filter away events before " + firstTimestamp.get());
            stackEvents = stackEvents.stream()
                                     .filter(stackEvent -> stackEvent.timestamp()
                                                                     .isAfter(firstTimestamp.get()) || stackEvent.timestamp()
                                                                                                                 .equals(firstTimestamp.get()))
                                     .collect(Collectors.toList());
        }

        Predicate<StackEvent> standardErrors = stackEvent -> stackEvent.resourceStatus()
                                                                       .equals(UPDATE_FAILED) ||
                                                             stackEvent.resourceStatus()
                                                                       .equals(CREATE_FAILED);
        return getError(stackEvents, standardErrors)
                .orElse(getError(stackEvents,
                                 stackEvent -> stackEvent.resourceStatusAsString()
                                                         .equals("UPDATE_ROLLBACK_IN_PROGRESS") || stackEvent.resourceStatusAsString().equals("ROLLBACK_IN_PROGRESS"))
                                .orElse(StackError.defaultError()));
    }

    private static Optional<Instant> getTimeForFirstEvent(List<StackEvent> stackEvents, String stackName) {
        return stackEvents.stream()
                          .filter(stackEvent -> stackEvent.resourceType().equals("AWS::CloudFormation::Stack"))
                          .filter(stackEvent -> stackEvent.logicalResourceId().equals(stackName))
                          .filter(stackEvent -> stackEvent.resourceStatus()
                                                          .equals(UPDATE_IN_PROGRESS))
                          .map(StackEvent::timestamp)
                          .max(Instant::compareTo);
    }

    private List<StackEvent> getStackEvents(CloudFormationEvent cloudFormationEvent) {
        DescribeStackEventsResponse response = cloudFormationClientFactory.getClient(cloudFormationEvent)
                                                                          .describeStackEvents(
                                                                                  DescribeStackEventsRequest.builder()
                                                                                                            .stackName(
                                                                                                                    cloudFormationEvent.getStackId()
                                                                                                                                  .orElseGet(
                                                                                                                                          cloudFormationEvent::getStackName))
                                                                                                            .build());
        List<StackEvent> stackEvents = new ArrayList<>(response.stackEvents());
        while (shouldGetAnotherBatch(cloudFormationEvent.getStackName(), response)) {
            logger.info("Getting another batch of events");
            response = getNextBatch(cloudFormationEvent, response.nextToken());
            stackEvents.addAll(response.stackEvents());
        }
        return stackEvents;
    }

    private boolean shouldGetAnotherBatch(String stackName, DescribeStackEventsResponse response) {
        return response.nextToken() != null && !containsInitEvent(stackName, response.stackEvents());
    }

    private DescribeStackEventsResponse getNextBatch(CloudFormationEvent cloudFormationEvent, String token) {
        return cloudFormationClientFactory.getClient(cloudFormationEvent).describeStackEvents(
                DescribeStackEventsRequest.builder()
                                          .stackName(cloudFormationEvent.getStackName())
                                          .nextToken(token)
                                          .build());
    }

    private boolean containsInitEvent(String stackName, List<StackEvent> events) {
        return events
                .stream()
                .anyMatch(stackEvent -> stackEvent.logicalResourceId().equals(stackName) && INIT_STATUSES.contains(
                        stackEvent.resourceStatus()));
    }


    private Optional<StackError> getError(List<StackEvent> events, Predicate<StackEvent> filter) {
        return events.stream()
                     .filter(filter)
                     .min(Comparator.comparingLong(o -> o.timestamp().toEpochMilli()))
                     .map(event -> StackError.builder()
                                             .setErrorStatus(event.resourceStatusAsString())
                                             .setResourceId(event.logicalResourceId())
                                             .setMessage(event.resourceStatusReason())
                                             .build());

    }
}
