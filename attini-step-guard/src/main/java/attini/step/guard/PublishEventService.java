package attini.step.guard;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.StackData;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class PublishEventService {


    private final SnsClient snsClient;
    private final EnvironmentVariables environmentVariables;

    public PublishEventService(SnsClient snsClient, EnvironmentVariables environmentVariables) {
        this.snsClient = requireNonNull(snsClient, "snsClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }


    public void postStepCompleted(CloudFormationManualTriggerEvent stepGuardInput) {

        postStepCompleted(stepGuardInput, null);

    }

    public void postStepCompleted(CloudFormationManualTriggerEvent cloudFormationManualTriggerEvent, StackError stackError) {

        AttiniContext attiniContext = cloudFormationManualTriggerEvent.getAttiniContext();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        String type = DesiredState.DELETED == cloudFormationManualTriggerEvent.getDesiredState() ? "DeleteCfn" : "DeployCfn";
        objectNode.put("type", type);
        objectNode.put("environment", attiniContext.getEnvironment().asString());
        objectNode.put("distributionName", attiniContext.getDistributionName().asString());
        objectNode.put("stepName", cloudFormationManualTriggerEvent.getStepName());
        objectNode.put("distributionId", attiniContext.getDistributionId().asString());
        objectNode.put("stackName", cloudFormationManualTriggerEvent.getStackName());
        objectNode.put("eventId", UUID.randomUUID().toString());

        MessageAttributeValue.Builder statusAttribute = MessageAttributeValue.builder()
                                                                             .dataType("String");
        if (stackError == null) {
            objectNode.put("status", "SUCCEEDED");
            statusAttribute.stringValue("SUCCEEDED");
        } else {
            objectNode.put("status", "FAILED");
            statusAttribute.stringValue("FAILED");
            objectNode.put("error", stackError.getMessage());
        }

        snsClient.publish(PublishRequest.builder()
                                        .message(objectNode.toString())
                                        .topicArn(environmentVariables.getDeploymentStatusTopic())
                                        .messageAttributes(Map.of("status",
                                                                  statusAttribute.build(),
                                                                  "type",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(type)
                                                                                       .build(),
                                                                  "environment",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(attiniContext.getEnvironment().asString())
                                                                                       .build(),
                                                                  "distributionName",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(attiniContext.getDistributionName().asString())
                                                                                       .build()))
                                        .build());

    }


    public void postStepCompleted(StackData stackData, String stackName) {

        postStepCompleted(stackData, stackName, null);

    }

    public void postStepCompleted(StackData stackData, String stackName, StackError stackError) {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();

        String type = DesiredState.DELETED == stackData.getDesiredState() ? "DeleteCfn" : "DeployCfn";
        objectNode.put("type", type);
        objectNode.put("environment", stackData.getEnvironment().asString());
        objectNode.put("distributionName", stackData.getDistributionName().asString());
        objectNode.put("stepName", stackData.getStepName());
        objectNode.put("distributionId", stackData.getDistributionId().asString());
        objectNode.put("eventId", UUID.randomUUID().toString());
        objectNode.put("stackName", stackName);


        MessageAttributeValue.Builder statusAttribute = MessageAttributeValue.builder()
                                                                             .dataType("String");
        if (stackError == null) {
            objectNode.put("status", "SUCCEEDED");
            statusAttribute.stringValue("SUCCEEDED");

        } else {
            objectNode.put("status", "FAILED");
            statusAttribute.stringValue("FAILED");
            objectNode.put("error", stackError.getMessage());
        }

        snsClient.publish(PublishRequest.builder()
                                        .message(objectNode.toString())
                                        .topicArn(environmentVariables.getDeploymentStatusTopic())
                                        .messageAttributes(Map.of("status",
                                                                  statusAttribute.build(),
                                                                  "type",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(type)
                                                                                       .build(),
                                                                  "environment",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(stackData.getEnvironment().asString())
                                                                                       .build(),
                                                                  "distributionName",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(stackData.getDistributionName().asString())
                                                                                       .build()))
                                        .build());

    }
}
