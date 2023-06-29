package attini.action.facades.stackdata;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import attini.action.domain.DeploymentPlanStateData;
import attini.action.facades.deployorigin.DeploymentName;
import attini.action.system.EnvironmentVariables;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DeploymentPlanDataDynamoFacade implements DeploymentPlanDataFacade {


    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public DeploymentPlanDataDynamoFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    @Override
    public void saveFinalStatus(String sfnArn, String status, Instant startTime) {

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(Map.of("resourceType",
                                                               AttributeValue.builder().s("DeploymentPlan").build(),
                                                               "name", AttributeValue.builder().s(sfnArn).build()))
                                                   .attributeUpdates(Map.of("status",
                                                                            AttributeValueUpdate.builder()
                                                                                                .action(AttributeAction.PUT)
                                                                                                .value(AttributeValue.builder()
                                                                                                                     .s(status)
                                                                                                                     .build())
                                                                                                .build(),
                                                                            "startTime",
                                                                            AttributeValueUpdate.builder().value(
                                                                                    AttributeValue.builder()
                                                                                                  .n(String.valueOf(
                                                                                                          startTime.getEpochSecond()))
                                                                                                  .build())
                                                                                                .build()))
                                                   .build());

    }

    @Override
    public DeploymentPlanStateData getDeploymentPlan(String sfnArn) {
        Map<String, String> deployOriginSourceNames = new HashMap<>();
        GetItemRequest getItemRequest = GetItemRequest.builder()
                                                      .tableName(environmentVariables.getResourceStatesTableName())
                                                      .key(Map.of(
                                                              "resourceType",
                                                              AttributeValue.builder()
                                                                            .s("DeploymentPlan")
                                                                            .build(),
                                                              "name",
                                                              AttributeValue.builder().s(sfnArn).build()
                                                      ))
                                                      .build();
        GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
        deployOriginSourceNames.put("DistributionName", getItemResponse.item().get("distributionName").s());
        String environment = getItemResponse.item().get("environment").s();
        deployOriginSourceNames.put("EnvironmentName", environment);
        DeploymentName sourceName = DeploymentName.create(Environment.of( deployOriginSourceNames.get("EnvironmentName")), DistributionName.of(deployOriginSourceNames.get("DistributionName")));
        String payloadDefaults = getItemResponse.item().get("payloadDefaults") != null ? getItemResponse.item()
                                                                                          .get("payloadDefaults")
                                                                                          .s() : "{}"; //default value is strictly for backward compatability, would throw en exception if someone would rerun an old deployment plan


        return new DeploymentPlanStateData(sourceName, ObjectIdentifier.of( getItemResponse.item().get("attiniObjectIdentifier").s()), payloadDefaults,
                                           Environment.of(environment));

    }

}
