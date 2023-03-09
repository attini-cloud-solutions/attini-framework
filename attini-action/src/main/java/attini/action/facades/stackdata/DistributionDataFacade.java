package attini.action.facades.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import attini.action.domain.Distribution;
import attini.action.domain.DistributionDependency;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DistributionDataFacade {

    private final EnvironmentVariables environmentVariables;
    private final DynamoDbClient dynamoDbClient;

    public DistributionDataFacade(EnvironmentVariables environmentVariables,
                                  DynamoDbClient dynamoDbClient) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
    }

    public Distribution getDistribution(DistributionName distName, Environment environment) {
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                .tableName(environmentVariables.getResourceStatesTableName())
                                                                                .key(Map.of("resourceType",
                                                                                            AttributeValue.builder()
                                                                                                          .s("Distribution")
                                                                                                          .build(),
                                                                                            "name",
                                                                                            AttributeValue.builder()
                                                                                                          .s(environment.asString() + "-" + distName.asString())
                                                                                                          .build()))
                                                                                .build())
                                                         .item();

        List<DistributionDependency> dependencies = item.get("dependencies") == null ? null : item.get("dependencies")
                                                                                                  .l()
                                                                                                  .stream()
                                                                                                  .map(value -> new DistributionDependency(
                                                                                                          value.m()
                                                                                                               .get("distributionName")
                                                                                                               .s()))
                                                                                                  .collect(Collectors.toList());

        return Distribution.builder()
                           .distributionId(DistributionId.of(item.get("distributionId").s()))
                           .distributionName(DistributionName.of(item.get("distributionName").s()))
                           .environment(Environment.of(item.get("environment").s()))
                           .deploymentSourcePrefix(item.get("deploymentSourcePrefix").s())
                           .outputUrl(item.get("outputUrl") == null ? null : item.get("outputUrl").s())
                           .dependencies(dependencies)
                           .build();


    }

    public void updateDistributionOutput(DistributionName distName, Environment environment, String outputUrl) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(Map.of("resourceType",
                                                               AttributeValue.builder().s("Distribution").build(),
                                                               "name",
                                                               AttributeValue.builder()
                                                                             .s(environment.asString() + "-" + distName.asString())
                                                                             .build()))
                                                   .attributeUpdates(Map.of("outputUrl",
                                                                            AttributeValueUpdate.builder().value(
                                                                                                        AttributeValue.builder()
                                                                                                                      .s(outputUrl)
                                                                                                                      .build())
                                                                                                .build()))
                                                   .build());
    }
}
