package attini.step.guard.deploydata;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import attini.domain.ObjectIdentifier;
import attini.step.guard.cloudformation.CloudFormationSnsEventImpl;
import attini.step.guard.EnvironmentVariables;
import attini.step.guard.cloudformation.InitDeploySnsEvent;
import attini.step.guard.stackdata.InitDeployData;
import attini.step.guard.stackdata.ResourceState;
import attini.step.guard.stackdata.StackData;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DeployDataFacade {

    private static final Logger logger = Logger.getLogger(DeployDataFacade.class);


    private final EnvironmentVariables environmentVariables;
    private final DynamoDbClient dynamoDbClient;

    public DeployDataFacade(EnvironmentVariables environmentVariables, DynamoDbClient dynamoDbClient) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
    }

    public void addStackError(CloudFormationSnsEventImpl cloudFormationEvent, StackData resourceState, String error) {
        AttributeValue list = AttributeValue.builder()
                                            .l(AttributeValue.builder()
                                                             .m(Map.of("resourceName",
                                                                       AttributeValue.builder()
                                                                                     .s(cloudFormationEvent.getLogicalResourceId())
                                                                                     .build(),
                                                                       "resourceStatus",
                                                                       AttributeValue.builder()
                                                                                     .s(cloudFormationEvent.getResourceStatus())
                                                                                     .build(),
                                                                       "errorType",
                                                                       AttributeValue.builder()
                                                                                     .s("CloudFormation")
                                                                                     .build(),
                                                                       "error",
                                                                       AttributeValue.builder()
                                                                                     .s(error)
                                                                                     .build(),
                                                                       "stepName",
                                                                       AttributeValue.builder()
                                                                                     .s(resourceState.getStepName())
                                                                                     .build(),
                                                                       "stackName",
                                                                       AttributeValue.builder()
                                                                                     .s(cloudFormationEvent.getStackName())
                                                                                     .build(),
                                                                       "region",
                                                                       AttributeValue.builder()
                                                                                     .s(cloudFormationEvent.getRegion()
                                                                                                           .orElseGet(
                                                                                                                   environmentVariables::getRegion))
                                                                                     .build()))
                                                             .build()).build();


        String name = cloudFormationEvent.getStackName() +
                      "-" +
                      cloudFormationEvent.getRegion().orElseGet(environmentVariables::getRegion) +
                      "-" +
                      cloudFormationEvent.getExecutionRoleArn().map(s -> s.split(":")[4])
                                         .orElseGet(environmentVariables::getAccountId);


        String deployName = createDeployName(resourceState);
        getDeployTimes(deployName, resourceState.getObjectIdentifier())
                .stream()
                .map(deployTime -> createKey(deployName, deployTime))
                .forEach(key -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                                           .tableName(environmentVariables.getDeployOriginTableName())
                                                                           .key(key)
                                                                           .updateExpression(
                                                                                   "SET errors.#key = list_append(if_not_exists(errors.#key, :empty_list), :val)")
                                                                           .expressionAttributeNames(Map.of("#key",
                                                                                                            name))
                                                                           .expressionAttributeValues(Map.of(":val",
                                                                                                             list,
                                                                                                             ":empty_list",
                                                                                                             AttributeValue.builder()
                                                                                                                           .l(Collections.emptyList())
                                                                                                                           .build()))
                                                                           .build()));


    }

    public void addInitStackError(InitDeploySnsEvent cloudFormationEvent, ResourceState resourceState, String error) {

        logger.info("Saving init deploy stack errors to deployment data");
        AttributeValue list = AttributeValue.builder()
                                            .l(AttributeValue.builder()
                                                             .m(Map.of("resourceName",
                                                                       AttributeValue.builder()
                                                                                     .s(cloudFormationEvent.getLogicalResourceId())
                                                                                     .build(),
                                                                       "resourceStatus",
                                                                       AttributeValue.builder()
                                                                                     .s(cloudFormationEvent.getResourceStatus())
                                                                                     .build(),
                                                                       "error",
                                                                       AttributeValue.builder()
                                                                                     .s(error)
                                                                                     .build()))
                                                             .build()).build();

        String deployName = createDeployName(resourceState);
        getDeployTimes(deployName, resourceState.getObjectIdentifier())
                .stream()
                .map(deployTime -> createKey(deployName, deployTime))
                .forEach(key -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                                           .tableName(environmentVariables.getDeployOriginTableName())
                                                                           .key(key)
                                                                           .updateExpression(
                                                                                   "SET initStackErrors = list_append(initStackErrors, :val)")
                                                                           .expressionAttributeValues(Map.of(":val",
                                                                                                             list))
                                                                           .build()));


    }

    public void addDeploymentPlanData(InitDeployData initDeployData) {

        logger.info("Saving deployment plan data to deployment origin");

        HashMap<String, AttributeValueUpdate> updates = new HashMap<>();

        updates.put("deploymentPlanCount",
                    AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().n(String.valueOf(initDeployData.getSfnArns().size())).build())
                                        .build());

        if (!initDeployData.getSfnArns().isEmpty()){
            AttributeValue attributeValue = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                 .tableName(environmentVariables.getResourceStatesTableName())
                                                                                 .key(Map.of("resourceType",
                                                                                             AttributeValue.builder()
                                                                                                           .s("DeploymentPlan")
                                                                                                           .build(),
                                                                                             "name",
                                                                                             AttributeValue.builder().s(initDeployData.getSfnArns().get(0)).build()))
                                                                                 .build()).item().get("attiniSteps");

            if (attributeValue != null && attributeValue.hasL()){
                updates.put("attiniSteps", AttributeValueUpdate.builder().value(attributeValue).build());
            }
        }

        String deployName = createDeployName(initDeployData);
        getDeployTimes(deployName, initDeployData.getObjectIdentifier())
                .stream()
                .map(deployTime -> createKey(deployName, deployTime))
                .forEach(key -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                                           .tableName(environmentVariables.getDeployOriginTableName())
                                                                           .attributeUpdates(updates)
                                                                           .key(key)
                                                                     .build()));


    }

    public void addExecutionError(ResourceState resourceState,
                                  String errorMessage) {
        String deployName = createDeployName(resourceState);

        logger.info("Setting execution errors, deploymentName = " + deployName + ", objectIdentifier = " + resourceState.getObjectIdentifier().asString());


        AttributeValueUpdate errorMessageAttribute = AttributeValueUpdate.builder()
                                                                         .value(AttributeValue.builder()
                                                                                              .s(errorMessage)
                                                                                              .build())
                                                                         .build();
        AttributeValueUpdate errorCodeAttribute = AttributeValueUpdate.builder()
                                                                      .value(AttributeValue.builder()
                                                                                           .s("CloudformationError")
                                                                                           .build())
                                                                      .build();


        getDeployTimes(deployName, resourceState.getObjectIdentifier())
                .stream()
                .map(deployTime -> createKey(deployName, deployTime))
                .forEach(key -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                                           .tableName(environmentVariables.getDeployOriginTableName())
                                                                           .key(key)
                                                                           .attributeUpdates(Map.of(
                                                                                   "errorMessage",
                                                                                   errorMessageAttribute,
                                                                                   "errorCode",
                                                                                   errorCodeAttribute))
                                                                           .build()));
    }

    private String createDeployName(ResourceState resourceState) {
        return resourceState.getEnvironment().asString() + "-" + resourceState.getDistributionName().asString();
    }

    private static Map<String, AttributeValue> createKey(String sourceName, String deployTime) {
        return Map.of("deploymentName", AttributeValue.builder()
                                                      .s(sourceName).build(),
                      "deploymentTime", AttributeValue.builder()
                                                      .n(deployTime).build());

    }

    private List<String> getDeployTimes(String sourceName, ObjectIdentifier objectIdentifier) {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .indexName("objectIdentifier")
                                                .tableName(environmentVariables.getDeployOriginTableName())
                                                .keyConditionExpression(
                                                        "objectIdentifier=:v_objectIdentifier and deploymentName=:v_deployName")
                                                .expressionAttributeValues(Map.of(":v_objectIdentifier",
                                                                                  AttributeValue.builder()
                                                                                                .s(objectIdentifier.asString())
                                                                                                .build(),
                                                                                  ":v_deployName",
                                                                                  AttributeValue.builder()
                                                                                                .s(sourceName)
                                                                                                .build()))
                                                .projectionExpression("deploymentTime")
                                                .build();

        return dynamoDbClient.query(queryRequest)
                             .items()
                             .stream()
                             .map(valueMap -> valueMap.get("deploymentTime").n())
                             .collect(toList());


    }
}
