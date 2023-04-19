/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import deployment.plan.system.EnvironmentVariables;
import deployment.plan.transform.CfnString;
import deployment.plan.transform.Runner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DeployStatesFacade {

    private static final Logger logger = Logger.getLogger(DeployStatesFacade.class);

    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;
    private final ObjectMapper objectMapper;

    public DeployStatesFacade(DynamoDbClient dynamoDbClient,
                              EnvironmentVariables environmentVariables, ObjectMapper objectMapper) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public void saveDeploymentPlanState(DeploymentPlanResourceState deploymentPlanResourceState) {
        logger.info(deploymentPlanResourceState);
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("resourceType", AttributeValue.builder().s("DeploymentPlan").build());
        item.put("name", AttributeValue.builder().s(deploymentPlanResourceState.getSfnArn()).build());
        item.put("environment",
                 AttributeValue.builder().s(deploymentPlanResourceState.getAttiniEnvironmentName()).build());
        item.put("distributionName",
                 AttributeValue.builder().s(deploymentPlanResourceState.getAttiniDistributionName()).build());
        item.put("attiniObjectIdentifier",
                 AttributeValue.builder().s(deploymentPlanResourceState.getAttiniObjectIdentifier()).build());
        item.put("distributionId",
                 AttributeValue.builder().s(deploymentPlanResourceState.getAttiniDistributionId()).build());
        item.put("stackName", AttributeValue.builder().s(deploymentPlanResourceState.getStackName()).build());
        try {
            item.put("payloadDefaults",
                     AttributeValue.builder()
                                   .s(objectMapper.writeValueAsString(deploymentPlanResourceState.getPayloadDefaults()))
                                   .build());
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        if (!deploymentPlanResourceState.getAttiniSteps().isEmpty()) {
            item.put("attiniSteps",
                     AttributeValue.builder()
                                   .l(deploymentPlanResourceState.getAttiniSteps()
                                                                 .stream()
                                                                 .map(attiniStep -> AttributeValue.builder()
                                                                                                  .m(Map.of("name",
                                                                                                            AttributeValue.builder()
                                                                                                                          .s(attiniStep.name())
                                                                                                                          .build(),
                                                                                                            "type",
                                                                                                            AttributeValue.builder()
                                                                                                                          .s(attiniStep.type())
                                                                                                                          .build()))
                                                                                                  .build())
                                                                 .toList())
                                   .build());
        }


        dynamoDbClient.putItem(PutItemRequest.builder()
                                             .tableName(environmentVariables.getResourceStatesTableName())
                                             .item(item)
                                             .build());

    }

    public void saveRunnerState(Runner runner, DeploymentPlanResourceState deploymentPlanResourceState) {

        Map<String, AttributeValueUpdate> map = new HashMap<>();
        map.put("environment", stringUpdateAttribute(
                deploymentPlanResourceState.getAttiniEnvironmentName()));
        map.put("attiniObjectIdentifier", stringUpdateAttribute(
                deploymentPlanResourceState.getAttiniObjectIdentifier()));
        map.put("container", updateNullableCfnString(runner.getContainerName(), "ContainerName"));
        map.put("roleArn", updateNullableCfnString(runner.getRoleArn(), "RoleArn"));
        map.put("distributionId", stringUpdateAttribute(
                deploymentPlanResourceState.getAttiniDistributionId()));
        map.put("distributionName", stringUpdateAttribute(
                deploymentPlanResourceState.getAttiniDistributionName()));
        map.put("runnerName", stringUpdateAttribute(runner.getName()));
        map.put("stackName", stringUpdateAttribute(
                deploymentPlanResourceState.getStackName()));
        map.put("subnets", updateNullableCfnCommaSeperatedList(runner.getSubnets(), "Subnets"));
        map.put("securityGroups", updateNullableCfnCommaSeperatedList(runner.getSecurityGroups(), "SecurityGroups"));
        map.put("assignPublicIp", updateNotNullableCfnString(runner.getAssignPublicIp(), "AssignPublicIp"));
        map.put("maxConcurrentJobs", updateNullableCfnNumber(runner.getMaxConcurrentJobs(), "MaxConcurrentJobs"));
        map.put("idleTimeToLive", updateNullableCfnNumber(runner.getIdleTimeToLive(), "IdleTimeToLive"));
        map.put("jobTimeout", updateNullableCfnNumber(runner.getJobTimeout(), "JobTimeout"));
        map.put("logLevel",
                stringUpdateAttribute(runner.getLogLevel() == null ? null : runner.getLogLevel().asString()));
        map.put("taskDefinitionArn", updateNotNullableCfnString(runner.getTaskDefinitionArn(), "TaskDefinitionArn"));
        map.put("cluster", updateCfnStringWithDefault(runner.getCluster(), "EcsCluster", "attini-default"));
        map.put("sqsQueueUrl", stringUpdateAttribute(runner.getQueueUrl().asString()));
        map.put("startupCommandsTimeout",
                updateNullableCfnNumber(runner.getInstallationCommandsTimeout(), "CommandsTimeout"));
        map.put("attiniVersion", stringUpdateAttribute(environmentVariables.getAttiniVersion()));
        map.put("cpu", updateNullableCfnNumber(runner.getCpu(), "Cpu"));
        map.put("memory", updateNullableCfnNumber(runner.getMemory(), "Memory"));

        if (!runner.getInstallationCommands().isEmpty()) {
            map.put("startupCommands",
                    AttributeValueUpdate.builder()
                                        .value(AttributeValue.builder().l(runner.getInstallationCommands()
                                                                                .stream()
                                                                                .map(
                                                                                        CfnString::asString)
                                                                                .map(s -> AttributeValue.builder()
                                                                                                        .s(s)
                                                                                                        .build())
                                                                                .collect(
                                                                                        Collectors.toList())).build())
                                        .build());
        } else {
            map.put("startupCommands", AttributeValueUpdate.builder().action(AttributeAction.DELETE).build());
        }

        runner.getEc2Configuration()
              .ifPresentOrElse(ec2Configuration -> {
                                   Map<String, AttributeValue> ec2CondigMap = new HashMap<>();
                                   ec2CondigMap.put("instanceType", AttributeValue.builder()
                                                                                  .s(ec2Configuration.getInstanceType()
                                                                                                     .asString())
                                                                                  .build());
                                   ec2CondigMap.put("instanceProfile", AttributeValue.builder()
                                                                                     .s(ec2Configuration.getInstanceProfile()
                                                                                                        .asString())
                                                                                     .build());
                                   ec2CondigMap.put("ecsClientLogGroup", AttributeValue.builder()
                                                                                       .s(ec2Configuration.getEcsClientLogGroup()
                                                                                                          .asString())
                                                                                       .build());

                                   if (ec2Configuration.getAmi() != null) {
                                       ec2CondigMap.put("ami",
                                                        AttributeValue.builder().s(ec2Configuration.getAmi().asString()).build());
                                   }
                                   map.put("ec2Configuration",
                                           AttributeValueUpdate.builder()
                                                               .value(AttributeValue.builder()
                                                                                    .m(ec2CondigMap)
                                                                                    .build())
                                                               .build());
                               },
                               () -> map.put("ec2Configuration",
                                             AttributeValueUpdate.builder()
                                                                 .action(AttributeAction.DELETE)
                                                                 .build()));

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(Map.of("resourceType",
                                                               AttributeValue.builder().s("Runner").build(),
                                                               "name",
                                                               AttributeValue.builder()
                                                                             .s(deploymentPlanResourceState.getStackName() + "-" + runner.getName())
                                                                             .build()))
                                                   .attributeUpdates(map)
                                                   .build());

    }

    private AttributeValueUpdate updateNotNullableCfnString(CfnString cfnString, String fieldName) {
        if (cfnString == null || !cfnString.isString()) {
            throw new IllegalArgumentException(fieldName + " must be a String");

        }
        return stringUpdateAttribute(cfnString.asString());
    }

    private AttributeValueUpdate updateNullableCfnCommaSeperatedList(CfnString cfnString, String fieldName) {
        if (cfnString == null) {
            return stringUpdateAttribute(null);
        }
        if (!cfnString.isString()) {
            throw new IllegalArgumentException(fieldName + " must be a comma seperated string. Current value = " + cfnString.getUncheckedValue());

        }
        return setUpdateAttribute(List.of(cfnString.asString().split(",")));
    }

    private AttributeValueUpdate updateNullableCfnString(CfnString cfnString, String fieldName) {
        if (cfnString == null) {
            return stringUpdateAttribute(null);
        }

        if (!cfnString.isString()) {
            throw new IllegalArgumentException(fieldName + " must be a String or omitted completely. Current value = " + cfnString.getUncheckedValue());
        }


        return stringUpdateAttribute(cfnString.asString());
    }

    private AttributeValueUpdate updateCfnStringWithDefault(CfnString cfnString,
                                                            String fieldName,
                                                            String defaultValue) {
        if (cfnString == null) {
            return stringUpdateAttribute(defaultValue);
        }

        if (!cfnString.isString()) {
            throw new IllegalArgumentException(fieldName + " must be a String or omitted completely. Current value = " + cfnString.getUncheckedValue());
        }


        return stringUpdateAttribute(cfnString.asString());
    }


    private AttributeValueUpdate setUpdateAttribute(Collection<String> value) {
        return AttributeValueUpdate.builder()
                                   .value(AttributeValue.builder()
                                                        .ss(value)
                                                        .build())
                                   .build();
    }


    private AttributeValueUpdate updateNullableCfnNumber(CfnString cfnString, String fieldName) {
        if (cfnString == null) {
            return numberUpdateAttribute(null);
        }

        try {
            Integer.parseInt(cfnString.asString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a positive number without decimals. Current value: " + cfnString.getUncheckedValue()
                                                                                                                                     .asText());
        }

        return numberUpdateAttribute(cfnString.asString());
    }


    private AttributeValueUpdate numberUpdateAttribute(String value) {

        if (value == null) {
            return AttributeValueUpdate.builder()
                                       .action(AttributeAction.DELETE)
                                       .build();
        }

        return AttributeValueUpdate.builder()
                                   .value(AttributeValue.builder()
                                                        .n(value)
                                                        .build())
                                   .build();
    }

    private AttributeValueUpdate stringUpdateAttribute(String value) {

        if (value == null) {
            return AttributeValueUpdate.builder()
                                       .action(AttributeAction.DELETE)
                                       .build();
        }
        return AttributeValueUpdate.builder()
                                   .value(AttributeValue.builder()
                                                        .s(value)
                                                        .build())
                                   .build();
    }

    public InitStackResourceState getInitStackState(String stackName) {
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                .tableName(environmentVariables.getResourceStatesTableName())
                                                                                .key(createTriggerDynamoKey(stackName))
                                                                                .build()).item();
        return InitStackResourceState.builder()
                                     .setDistributionId(item.get("distributionId").s())
                                     .setDistributionName(item.get("distributionName").s())
                                     .setEnvironment(item.get("environment").s())
                                     .setObjectIdentifier(item.get("attiniObjectIdentifier").s())
                                     .build();
    }

    public void deleteDeploymentPlanState(String sfnArn) {
        logger.info("Deleting OriginDeployDataLink");
        try {
            dynamoDbClient.deleteItem(DeleteItemRequest.builder()
                                                       .tableName(environmentVariables.getResourceStatesTableName())
                                                       .key(Map.of(
                                                               "resourceType",
                                                               AttributeValue.builder()
                                                                             .s("DeploymentPlan")
                                                                             .build(),
                                                               "name",
                                                               AttributeValue.builder()
                                                                             .s(sfnArn)
                                                                             .build()))
                                                       .build());
        } catch (ResourceNotFoundException e) {
            logger.warn("OriginDeployDataLink was already deleted");
        }

    }

    public void removeErrors(String stackName) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(createTriggerDynamoKey(stackName))
                                                   .updateExpression("Remove stackError")
                                                   .build());
    }

    public void saveSfnTrigger(String sfnArn, String stackName) {
        logger.info("Saving sfn arn: " + sfnArn);
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(createTriggerDynamoKey(stackName))
                                                   .updateExpression("Add sfnArns :val")
                                                   .expressionAttributeValues(Map.of(":val",
                                                                                     AttributeValue.builder()
                                                                                                   .ss(sfnArn)
                                                                                                   .build()))
                                                   .build());
    }

    public void saveToInitData(List<String> runnerName, String stackName, Map<String, String> parameters) {
        logger.info("Saving runner names to init stack data: " + runnerName);

        if (!runnerName.isEmpty()) {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                       .tableName(environmentVariables.getResourceStatesTableName())
                                                       .key(createTriggerDynamoKey(stackName))
                                                       .updateExpression(
                                                               "ADD runners :runners SET stackParameters = :params")
                                                       .expressionAttributeValues(Map.of(":runners",
                                                                                         AttributeValue.builder()
                                                                                                       .ss(runnerName)
                                                                                                       .build(),
                                                                                         ":params",
                                                                                         AttributeValue.builder()
                                                                                                       .m(parameters.entrySet()
                                                                                                                    .stream()
                                                                                                                    .collect(
                                                                                                                            Collectors.toMap(
                                                                                                                                    Map.Entry::getKey,
                                                                                                                                    entry -> AttributeValue.builder()
                                                                                                                                                           .s(entry.getValue())
                                                                                                                                                           .build())))
                                                                                                       .build()))
                                                       .build());

        } else {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                       .tableName(environmentVariables.getResourceStatesTableName())
                                                       .key(createTriggerDynamoKey(stackName))
                                                       .updateExpression("SET stackParameters = :params")
                                                       .expressionAttributeValues(Map.of(":params",
                                                                                         AttributeValue.builder()
                                                                                                       .m(parameters.entrySet()
                                                                                                                    .stream()
                                                                                                                    .collect(
                                                                                                                            Collectors.toMap(
                                                                                                                                    Map.Entry::getKey,
                                                                                                                                    entry -> AttributeValue.builder()
                                                                                                                                                           .s(entry.getValue())
                                                                                                                                                           .build())))
                                                                                                       .build()))
                                                       .build());
        }


    }

    public void deleteSfnTrigger(String sfnArn, String stackName) {
        logger.info("Deleting sfn arn: " + sfnArn);
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(createTriggerDynamoKey(stackName))
                                                   .updateExpression("DELETE sfnArns :val")
                                                   .expressionAttributeValues(Map.of(":val",
                                                                                     AttributeValue.builder()
                                                                                                   .ss(sfnArn)
                                                                                                   .build()))
                                                   .build());
    }


    private Map<String, AttributeValue> createTriggerDynamoKey(String stackName) {
        return Map.of(
                "resourceType", AttributeValue.builder().s("InitDeployCloudformationStack").build(),
                "name", AttributeValue.builder().s(stackName).build());
    }

}
