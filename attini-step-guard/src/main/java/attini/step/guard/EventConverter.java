package attini.step.guard;

import static java.util.stream.Collectors.toMap;

import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import attini.step.guard.cloudformation.CloudFormationManualTriggerEvent;
import attini.step.guard.cloudformation.CloudFormationSnsEventImpl;
import attini.step.guard.cloudformation.InitDeploySnsEvent;
import attini.step.guard.stackdata.DesiredState;

public class EventConverter {

    public static InitDeployManualTriggerEvent createManualInitDeployInput(JsonNode input) {
        return new InitDeployManualTriggerEvent(input.get("stackName").textValue());
    }


    public static CloudFormationManualTriggerEvent createManualTriggerInput(JsonNode input) {
        JsonNode payload = input.get("payload");

        return CloudFormationManualTriggerEvent.builder()
                                               .setStackName(payload.get("stackName").textValue())
                                               .setResourceStatus(payload.get("resourceStatus").textValue())
                                               .setRegion(payload.get("region").textValue())
                                               .setExecutionRoleArn(payload.get("executionRoleArn").textValue())
                                               .setStackId(payload.get("stackId").textValue())
                                               .setSfnResponseToken(payload.get("sfnResponseToken").textValue())
                                               .setStepName(payload.get("stepName").textValue())
                                               .setOutputPath(payload.get("outputPath").textValue())
                                               .setClientRequestToken(payload.get("clientRequestToken").textValue())
                                               .setDesiredState(DesiredState.valueOf(payload.get("desiredState")
                                                                                            .textValue()))
                                               .setLogicalResourceId(payload.get("logicalResourceId").textValue())
                                               .setAttiniContext(AttiniContext.builder()
                                                                              .setDistributionId(DistributionId.of(
                                                                                      payload.get("distributionId")
                                                                                             .textValue()))
                                                                              .setDistributionName(DistributionName.of(
                                                                                      payload.get(
                                                                                                     "distributionName")
                                                                                             .textValue()))
                                                                              .setEnvironment(Environment.of(payload.get(
                                                                                                                            "environment")
                                                                                                                    .textValue()))
                                                                              .setObjectIdentifier(ObjectIdentifier.of(
                                                                                      payload.get(
                                                                                                     "objectIdentifier")
                                                                                             .textValue()))
                                                                              .build())
                                               .build();
    }

    public static InitDeploySnsEvent createInitDeployInput(JsonNode input) {
        JsonNode jsonNode = input.get("Records")
                                 .get(0)
                                 .get("Sns");

        Map<String, String> message = createMessageMap(jsonNode.get("Message"));

        InitDeploySnsEvent.Builder builder = InitDeploySnsEvent.builder()
                                                               .setStackName(message.get("StackName"))
                                                               .setResourceStatus(message.get("ResourceStatus"))
                                                               .setLogicalResourceId(message.get("LogicalResourceId"))
                                                               .setResourceType(message.get("ResourceType"))
                                                               .setClientRequestToken(message.get("ClientRequestToken"))
                                                               .setStackId(message.get("StackId"));

        if (message.get("ResourceStatusReason") != null && !message.get("ResourceStatusReason").isEmpty()) {
            builder.setResourceStatusReason(message.get("ResourceStatusReason"));
        }

        return builder.build();

    }

    public static ManualApprovalEvent createManualApprovalEvent(JsonNode input) {
        JsonNode jsonNode = input.get("Records")
                                 .get(0)
                                 .get("Sns");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode message = objectMapper.readTree(jsonNode.get("Message").asText());
            return ManualApprovalEvent.builder()
                                      .distributionName(DistributionName.of(message.get("distributionName").asText()))
                                      .stepName(message.get("stepName").asText())
                                      .environment(Environment.of(message.get("environment").asText()))
                                      .sfnToken(message.get("sfnToken").asText())
                                      .message(message.has("message") ? message.get("message").asText() : null)
                                      .abort(message.get("abort").asBoolean())
                                      .build();

        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }


    }

    public static CloudFormationSnsEventImpl createSnsEvent(JsonNode input) {
        JsonNode jsonNode = input.get("Records")
                                 .get(0)
                                 .get("Sns");
        Map<String, String> message = createMessageMap(jsonNode.get("Message"));

        CloudFormationSnsEventImpl.Builder builder = CloudFormationSnsEventImpl.builder()
                                                                               .setStackName(message.get("StackName"))
                                                                               .setResourceStatus(message.get("ResourceStatus"))
                                                                               .setLogicalResourceId(message.get(
                                                                               "LogicalResourceId"))
                                                                               .setResourceType(message.get("ResourceType"))
                                                                               .setClientRequestToken(message.get(
                                                                               "ClientRequestToken"))
                                                                               .setStackId(message.get("StackId"));

        if (message.get("ResourceStatusReason") != null && !message.get("ResourceStatusReason").isEmpty()) {
            builder.setResourceStatusReason(message.get("ResourceStatusReason"));
        }
        return builder.build();

    }

    private static Map<String, String> createMessageMap(JsonNode message) {
        return Arrays.stream(message.asText()
                                    .split("'\\n"))
                     .filter(s -> s.split("=").length > 1)
                     .collect(toMap(s -> s.split("=")[0],
                                    s -> s.split("=")[1].replace("'", "")));
    }

}
