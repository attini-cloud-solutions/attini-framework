/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.facades.stepfunction;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.ExecutionListItem;
import software.amazon.awssdk.services.sfn.model.ExecutionStatus;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryRequest;
import software.amazon.awssdk.services.sfn.model.GetExecutionHistoryResponse;
import software.amazon.awssdk.services.sfn.model.HistoryEvent;
import software.amazon.awssdk.services.sfn.model.ListExecutionsRequest;
import software.amazon.awssdk.services.sfn.model.ListExecutionsResponse;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;
import software.amazon.awssdk.services.sfn.model.StopExecutionRequest;

public class StepFunctionFacade {

    private static final Logger logger = Logger.getLogger(StepFunctionFacade.class);


    private final SfnClient sfnClient;

    public StepFunctionFacade(SfnClient sfnClient) {
        this.sfnClient = requireNonNull(sfnClient, "sfnClient");
    }

    public Stream<String> listExecutions(String sfnArn) {
        ListExecutionsResponse listExecutionsResponse = sfnClient
                .listExecutions(ListExecutionsRequest.builder()
                                                     .stateMachineArn(sfnArn)
                                                     .statusFilter(ExecutionStatus.RUNNING)
                                                     .build());
        return listExecutionsResponse.executions()
                                     .stream()
                                     .map(ExecutionListItem::executionArn);
    }

    public ExecutionSummery getExecutionSummery(String executionArn) {

        logger.info("Creating execution summery for executionArn=" + executionArn);

        List<HistoryEvent> historyEvents = getStateExitedEvents(executionArn);

        logger.info("All events for execution retrieved");

        String output = historyEvents.stream()
                                     .filter(historyEvent -> historyEvent.stateExitedEventDetails()
                                                                         .name()
                                                                         .equals("AttiniPrepareDeployment"))
                                     .findAny()
                                     .orElseThrow(() -> new RuntimeException("No AttiniPrepareDeployment step found"))
                                     .stateExitedEventDetails()
                                     .output();

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(output);

            DistributionName distributionName = DistributionName.of(jsonNode.get("deploymentOriginData")
                                                                            .get("distributionName")
                                                                            .asText());
            Environment environment = Environment.of(jsonNode.get("deploymentOriginData").get("environment").asText());

            DistributionId distributionId = DistributionId.of(jsonNode.get("deploymentOriginData")
                                                                      .get("distributionId")
                                                                      .asText());
            ObjectIdentifier objectIdentifier = ObjectIdentifier.of(jsonNode.get("deploymentOriginData")
                                                                            .get("objectIdentifier")
                                                                            .asText());


            return ExecutionSummery.builder()
                                   .setNrOfSteps(historyEvents.size())
                                   .setEnvironment(environment)
                                   .setDistributionName(distributionName)
                                   .setDistributionId(distributionId)
                                   .setObjectIdentifier(objectIdentifier)
                                   .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not parse json", e);
        }

    }

    private List<HistoryEvent> getStateExitedEvents(String executionArn) {
        GetExecutionHistoryResponse executionHistory =
                sfnClient.getExecutionHistory(GetExecutionHistoryRequest.builder()
                                                                        .executionArn(executionArn)
                                                                        .build());

        List<HistoryEvent> historyEvents = new ArrayList<>(executionHistory.events());
        while (executionHistory.nextToken() != null) {
            logger.info("More events present, getting another batch");
            executionHistory = sfnClient.getExecutionHistory(GetExecutionHistoryRequest.builder()
                                                                                       .executionArn(
                                                                                               executionArn)
                                                                                       .nextToken(executionHistory.nextToken())
                                                                                       .build());
            historyEvents.addAll(executionHistory.events());

        }

        return historyEvents.stream().filter(historyEvent -> historyEvent.stateExitedEventDetails() != null)
                            .collect(Collectors.toList());
    }

    public void stopExecution(String executionArn, String cause) {
        sfnClient.stopExecution(StopExecutionRequest.builder()
                                                    .executionArn(executionArn)
                                                    .cause(cause)
                                                    .build());
    }

    public void sendError(String token, String errorMessage, String error) {
        sfnClient.sendTaskFailure(SendTaskFailureRequest.builder()
                                                        .taskToken(token)
                                                        .cause(errorMessage)
                                                        .error(error)
                                                        .build());
    }

    public void sendSuccess(String token, String output) {
        sfnClient.sendTaskSuccess(SendTaskSuccessRequest.builder()
                                                        .taskToken(token)
                                                        .output(output)
                                                        .build());
    }
}
