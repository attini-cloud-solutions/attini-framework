package attini.action.facades.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class InitStackDataDynamoFacade implements InitStackDataFacade {

    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public InitStackDataDynamoFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    @Override
    public Map<String, String> getInitConfigVariables(String initStackName) {
        AttributeValue attributeValue = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                             .tableName(environmentVariables.getResourceStatesTableName())
                                                                             .key(Map.of("resourceType",
                                                                                         AttributeValue.builder()
                                                                                                       .s("InitDeployCloudformationStack")
                                                                                                       .build(),
                                                                                         "name",
                                                                                         AttributeValue.builder()
                                                                                                       .s(initStackName)
                                                                                                       .build()))
                                                                             .build())
                                                      .item()
                                                      .get("variables");
        if (attributeValue == null) {
            return Collections.emptyMap();
        }
        return attributeValue.m()
                             .entrySet()
                             .stream()
                             .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().s()));
    }

    @Override
    public Map<String, String> getInitStackParameters(String initStackName) {
        AttributeValue stackParameters = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                              .tableName(environmentVariables.getResourceStatesTableName())
                                                                              .key(Map.of("resourceType",
                                                                                          AttributeValue.builder()
                                                                                                        .s("InitDeployCloudformationStack")
                                                                                                        .build(),
                                                                                          "name",
                                                                                          AttributeValue.builder()
                                                                                                        .s(initStackName)
                                                                                                        .build()))
                                                                              .build())
                                                       .item()
                                                       .get("stackParameters");

        if (stackParameters == null) {
            return Collections.emptyMap(); //  null check to maintain backwards compatability
        }
        return stackParameters.m()
                              .entrySet()
                              .stream()
                              .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().s()));
    }
}
