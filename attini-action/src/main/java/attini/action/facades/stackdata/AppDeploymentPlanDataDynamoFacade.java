package attini.action.facades.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.stream.Collectors;

import attini.action.system.EnvironmentVariables;
import attini.domain.Environment;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class AppDeploymentPlanDataDynamoFacade implements AppDeploymentPlanDataFacade {
    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public AppDeploymentPlanDataDynamoFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = environmentVariables;
    }

    @Override
    public Map<String, String> getStackParameters(String appDeploymentPlan, Environment environment) {
        AttributeValue stackParameters = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                              .tableName(environmentVariables.getResourceStatesTableName())
                                                                              .key(Map.of("resourceType",
                                                                                          AttributeValue.builder()
                                                                                                        .s("AppDeploymentPlan")
                                                                                                        .build(),
                                                                                          "name",
                                                                                          AttributeValue.builder()
                                                                                                        .s("%s-%s".formatted(
                                                                                                                environment.asString(),
                                                                                                                appDeploymentPlan))
                                                                                                        .build()))
                                                                              .build())
                                                       .item()
                                                       .get("stackParameters");

        return stackParameters.m()
                              .entrySet()
                              .stream()
                              .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().s()));
    }
}

