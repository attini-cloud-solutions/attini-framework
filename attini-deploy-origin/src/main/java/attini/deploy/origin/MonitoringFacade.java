package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.Environment;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

public class MonitoringFacade {

    private final SnsClient snsClient;
    private final EnvironmentVariables environmentVariables;
    private final ObjectMapper objectMapper;

    public MonitoringFacade(SnsClient snsClient,
                            EnvironmentVariables environmentVariables,
                            ObjectMapper objectMapper) {
        this.snsClient = requireNonNull(snsClient, "snsClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public void sendInitDeployEvent(DistributionData distributionData, Environment environment) {

        HashMap<Object, Object> map = new HashMap<>();

        map.put("type", "InitDeploy");
        map.put("distributionId", distributionData.getAttiniConfig().getAttiniDistributionId().asString());
        map.put("distributionName", distributionData.getAttiniConfig().getAttiniDistributionName().asString());
        map.put("eventId", UUID.randomUUID().toString());
        map.put("environment",environment.asString());
        map.put("status", "SUCCEEDED");
        map.put("tags", distributionData.getAttiniConfig().getAttiniDistributionTags());
        distributionData.getAttiniConfig()
                        .getAttiniInitDeployStackConfig()
                        .ifPresent(attiniInitDeployStackConfig -> map.put("initStackName",
                                                                          attiniInitDeployStackConfig.getInitDeployStackName()));


        snsClient.publish(PublishRequest.builder()
                                        .topicArn(environmentVariables.getDeploymentStatusTopic())
                                        .messageAttributes(Map.of("status",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue("SUCCEEDED")
                                                                                       .build(),
                                                                  "type",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue("InitDeploy")
                                                                                       .build(),
                                                                  "environment",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(environment.asString())
                                                                                       .build(),
                                                                  "distributionName",
                                                                  MessageAttributeValue.builder()
                                                                                       .dataType("String")
                                                                                       .stringValue(distributionData.getAttiniConfig().getAttiniDistributionName().asString())
                                                                                       .build()))
                                        .message(toJsonString(map))
                                        .build());


    }

    private String toJsonString(HashMap<Object, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse json", e);
        }
    }
}
