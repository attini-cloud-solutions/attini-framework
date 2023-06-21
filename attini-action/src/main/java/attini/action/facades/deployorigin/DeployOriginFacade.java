/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.facades.deployorigin;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import attini.action.system.EnvironmentVariables;
import attini.domain.DeployOriginData;
import attini.domain.DistributionContext;
import attini.domain.DistributionContextImpl;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import attini.domain.Version;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;


public class DeployOriginFacade {
    private static final Logger logger = Logger.getLogger(DeployOriginFacade.class);
    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public DeployOriginFacade(DynamoDbClient dynamoDbClient,
                              EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }


    public void setDeploymentPlanStatus(String deployName, ObjectIdentifier objectIdentifier, String status) {

        logger.info("Setting execution status: " + status);


        AttributeValueUpdate statusAttribute = AttributeValueUpdate.builder()
                                                                   .value(AttributeValue.builder()
                                                                                        .s(status)
                                                                                        .build())
                                                                   .build();


        getDeployTimes(deployName, objectIdentifier)
                .stream()
                .map(deployTimeAndArns -> deployTimeAndArns.deployTime)
                .map(deployTime -> createKey(deployName, deployTime))
                .forEach(key -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                                           .tableName(environmentVariables.getDeployOriginTableName())
                                                                           .key(key)
                                                                           .attributeUpdates(Map.of(
                                                                                   "deploymentPlanStatus",
                                                                                   statusAttribute))
                                                                           .build()));


    }

    public void setSfnExecutionArn(String executionArn,
                                   ObjectIdentifier objectIdentifier,
                                   String deploymentSourceName) {

        logger.info("Setting execution arn");


        List<DeployTimeAndArns> deployTimeAndArnsList = getDeployTimes(deploymentSourceName,
                                                                       objectIdentifier);
        String executionArnKey = createKey(executionArn);

        deployTimeAndArnsList
                .stream()
                .map(deployTimeAndArns -> deployTimeAndArns.deployTime)
                .map(deployTime -> createKey(deploymentSourceName, deployTime))
                .forEach(key -> dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                                           .tableName(environmentVariables.getDeployOriginTableName())
                                                                           .key(key)
                                                                           .updateExpression("SET executionArns.#n = :v")
                                                                           .expressionAttributeNames(Map.of("#n",
                                                                                                            executionArnKey))
                                                                           .expressionAttributeValues(Map.of(":v",
                                                                                                             AttributeValue.builder()
                                                                                                                           .s(executionArn)
                                                                                                                           .build()))
                                                                           .build()));


    }

    private String createKey(String executionArn) {
        String removedExecutionId = executionArn.substring(0, executionArn.lastIndexOf(":"));
        return removedExecutionId.substring(removedExecutionId.lastIndexOf(":") + 1);
    }

    public void addExecutionError(String deployName,
                                  ObjectIdentifier objectIdentifier,
                                  String errorMessage,
                                  String errorCode) {

        logger.info("Setting execution errors, deploymentName = " + deployName + ", objectIdentifier = " + objectIdentifier.asString());


        AttributeValueUpdate errorMessageAttribute = AttributeValueUpdate.builder()
                                                                         .value(AttributeValue.builder()
                                                                                              .s(errorMessage)
                                                                                              .build())
                                                                         .build();
        AttributeValueUpdate errorCodeAttribute = AttributeValueUpdate.builder()
                                                                      .value(AttributeValue.builder()
                                                                                           .s(errorCode)
                                                                                           .build())
                                                                      .build();


        getDeployTimes(deployName, objectIdentifier)
                .stream()
                .map(deployTimeAndArns -> deployTimeAndArns.deployTime)
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

    public DistributionContext getContext(String executionArn, String deploymentName) {
        QueryRequest queryRequest = QueryRequest.builder()
                                                .tableName(environmentVariables.getDeployOriginTableName())
                                                .keyConditionExpression(
                                                        "deploymentName=:v_deployName")
                                                .expressionAttributeValues(Map.of(
                                                        ":v_deployName",
                                                        AttributeValue.builder()
                                                                      .s(deploymentName)
                                                                      .build()))
                                                .build();

        Map<String, AttributeValue> deployData = dynamoDbClient.query(queryRequest)
                                                               .items()
                                                               .stream()
                                                               .filter(map -> map.get("executionArns")
                                                                                 .m()
                                                                                 .values()
                                                                                 .stream()
                                                                                 .map(AttributeValue::s)
                                                                                 .toList()
                                                                                 .contains(executionArn))
                                                               .findAny()
                                                               .orElse(Collections.emptyMap());

        return DistributionContextImpl.builder()
                                      .distributionName(DistributionName.of(deployData.get("distributionName").s()))
                                      .distributionId(DistributionId.of(deployData.get("distributionId").s()))
                                      .environment(Environment.of(deployData.get("environment").s()))
                                      .objectIdentifier(ObjectIdentifier.of(deployData.get("objectIdentifier").s()))
                                      .build();

    }

    public Set<String> getLatestExecutionArns(String deploymentName) {

        logger.info("Getting latest executions for deployment: " + deploymentName);
        return dynamoDbClient.query(QueryRequest.builder()
                                                .consistentRead(true)
                                                .tableName(environmentVariables.getDeployOriginTableName())
                                                .keyConditionExpression("deploymentName = :v_deploymentName")
                                                .expressionAttributeValues(Map.of(":v_deploymentName",
                                                                                  AttributeValue.fromS(deploymentName)))
                                                .limit(5)
                                                .scanIndexForward(false)
                                                .build())
                             .items()
                             .stream()
                             .flatMap(stringAttributeValueMap -> stringAttributeValueMap.get("executionArns")
                                                                                        .m()
                                                                                        .values()
                                                                                        .stream())
                             .map(AttributeValue::s)
                             .collect(Collectors.toSet());
    }

    public DeployOriginData getDeployOriginData(ObjectIdentifier objectIdentifier, String deploymentName) {

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
                                                                                                .s(deploymentName)
                                                                                                .build()))
                                                .build();


        return dynamoDbClient.query(queryRequest)
                             .items()
                             .stream()
                             .filter(valueMap -> !valueMap.get("deploymentTime").n().equals("0"))
                             .findAny()
                             .map(this::toDeployOriginData)
                             .orElseThrow(() -> new IllegalStateException(
                                     "No deploy data found in DynamoDb for objectIdentifier=" + objectIdentifier
                                             .asString()));

    }

    private DeployOriginData toDeployOriginData(Map<String, AttributeValue> item) {

        return DeployOriginData.builder()
                               .deploySource(new DeployOriginData.DeploySource(item.get("deploymentSource")
                                                                                   .m()
                                                                                   .get("deploymentSourcePrefix")
                                                                                   .s(),
                                                                               item.get("deploymentSource")
                                                                                   .m()
                                                                                   .get("deploymentSourceBucket")
                                                                                   .s()))
                               .deployTimeInEpoch(Long.parseLong(item.get("deploymentTime").n()))
                               .deployName(item.get("deploymentName").s())
                               .stackName(item.get("stackName").s())
                               .distributionName(DistributionName.of(item.get("distributionName").s()))
                               .distributionId(DistributionId.of(item.get("distributionId").s()))
                               .objectIdentifier(ObjectIdentifier.of(item.get("objectIdentifier").s()))
                               .environment(Environment.of(item.get("environment").s()))
                               .version(item.get("version") != null ? Version.of(item.get("version").s()) : null)
                               .samPackaged(item.get("samPackaged") != null ? item.get("samPackaged").bool() : false)
                               .distributionTags(toMap(item.get("distributionTags").m()))
                               .executionArns(toMap(item.get("executionArns").m()))
                               .build();
    }

    private static Map<String, String> toMap(Map<String, AttributeValue> map) {
        return map.entrySet()
                  .stream()
                  .collect(Collectors.toMap(Map.Entry::getKey,
                                            entry -> entry.getValue().s()));
    }

    private static Map<String, AttributeValue> createKey(String sourceName, String deployTime) {
        return Map.of("deploymentName", AttributeValue.builder()
                                                      .s(sourceName)
                                                      .build(),
                      "deploymentTime", AttributeValue.builder()
                                                      .n(deployTime)
                                                      .build());
    }


    private List<DeployTimeAndArns> getDeployTimes(String sourceName, ObjectIdentifier objectIdentifier) {
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
                                                .projectionExpression("deploymentTime, executionArns")
                                                .build();

        return dynamoDbClient.query(queryRequest)
                             .items()
                             .stream()
                             .map(valueMap -> new DeployTimeAndArns(valueMap.get("deploymentTime").n(),
                                                                    valueMap.containsKey("executionArns") ? valueMap.get(
                                                                                                                            "executionArns")
                                                                                                                    .ss() : Collections.emptyList()))
                             .collect(toList());


    }

    private static class DeployTimeAndArns {
        final String deployTime;
        final List<String> executionArns;

        public DeployTimeAndArns(String deployTime, List<String> executionArns) {
            this.deployTime = deployTime;
            this.executionArns = executionArns;
        }
    }

}


