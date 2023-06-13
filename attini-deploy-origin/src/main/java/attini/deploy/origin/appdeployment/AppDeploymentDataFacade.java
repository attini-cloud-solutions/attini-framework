package attini.deploy.origin.appdeployment;

import java.util.Map;

import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.Environment;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class AppDeploymentDataFacade {

    private final EnvironmentVariables environmentVariables;
    private final DynamoDbClient dynamoDbClient;

    public AppDeploymentDataFacade(EnvironmentVariables environmentVariables,
                                   DynamoDbClient dynamoDbClient) {
        this.environmentVariables = environmentVariables;
        this.dynamoDbClient = dynamoDbClient;
    }

    public AppDeploymentData getSfnArn(Environment environment, String appDeploymentPlan) {

        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                .tableName(environmentVariables.getResourceStatesTableName())
                                                                                .key(Map.of("resourceType",
                                                                                            AttributeValue.fromS(
                                                                                                    "AppDeploymentPlan"),
                                                                                            "name",
                                                                                            AttributeValue.fromS("%s-%s".formatted(
                                                                                                    environment.asString(),
                                                                                                    appDeploymentPlan))))
                                                                                .build()).item();
        return new AppDeploymentData(item.get("sfnArn").s(),
                                     item.get("stackName").s());

    }

    public record AppDeploymentData(String sfnArn, String stackName) {

    }
}
