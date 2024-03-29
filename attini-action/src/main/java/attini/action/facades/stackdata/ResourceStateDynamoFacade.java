/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.facades.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import attini.action.actions.deploycloudformation.StackData;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.runner.Ec2;
import attini.action.actions.runner.RunnerData;
import attini.action.actions.runner.RunnerDataConverter;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.action.system.EnvironmentVariables;
import attini.domain.DeployOriginData;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class ResourceStateDynamoFacade implements ResourceStateFacade {


    private static final Logger logger = Logger.getLogger(ResourceStateDynamoFacade.class);

    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;


    public ResourceStateDynamoFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }


    @Override
    public RunnerData getRunnerData(String stackName, String runnerName) {

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                        .tableName(environmentVariables.getResourceStatesTableName())
                                                                        .key(createDynamoKey(stackName, runnerName))
                                                                        .build());


        Map<String, AttributeValue> item = response.item();
        if (item.isEmpty()){
            throw new IllegalArgumentException("No runner found with name "+ runnerName+ " for stack: " + stackName);
        }
        return RunnerDataConverter.convert(item);
    }

    @Override
    public boolean acquireEc2StartLock(RunnerData runnerData) {

        String executionId = runnerData.getStartedByExecutionArn().map(SfnExecutionArn::extractExecutionId).orElse("");
        String conditionExpression = "attribute_not_exists(#ec2Configuration.#startedByExecution)" + runnerData.getStartedByExecutionArn()
                                                                                                               .map(sfnExecutionArn -> " OR #ec2Configuration.#startedByExecution <> :v_executionId")
                                                                                                               .orElse("");
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                       .tableName(environmentVariables.getResourceStatesTableName())
                                                       .key(createDynamoKey(runnerData.getStackName(),
                                                                            runnerData.getRunnerName()))
                                                       .updateExpression(
                                                               "SET #ec2Configuration.#startedByExecution = :v_executionId")
                                                       .expressionAttributeNames(Map.of("#ec2Configuration",
                                                                                        "ec2Configuration",
                                                                                        "#startedByExecution",
                                                                                        "startedByExecution"))
                                                       .expressionAttributeValues(Map.of(":v_executionId",
                                                                                         AttributeValue.builder()
                                                                                                       .s(executionId)
                                                                                                       .build()))
                                                       .conditionExpression(
                                                               conditionExpression)
                                                       .build());

            return true;
        } catch (ConditionalCheckFailedException e) {
            logger.info("Failed to acquire lock for starting ec2 instance");
            return false;
        }

    }

    @Override
    public boolean acquireEcsStartLock(RunnerData runnerData) {
        String executionId = runnerData.getStartedByExecutionArn().map(SfnExecutionArn::extractExecutionId).orElse("");
        String conditionExpression = "attribute_not_exists(#taskStartedByExecution)" + runnerData.getStartedByExecutionArn()
                                                                                                 .map(sfnExecutionArn -> " OR #taskStartedByExecution <> :v_executionId")
                                                                                                 .orElse("");
        try {
            dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                       .tableName(environmentVariables.getResourceStatesTableName())
                                                       .key(createDynamoKey(runnerData.getStackName(),
                                                                            runnerData.getRunnerName()))
                                                       .updateExpression(
                                                               "SET #taskStartedByExecution = :v_executionId")
                                                       .expressionAttributeNames(Map.of("#taskStartedByExecution",
                                                                                        "taskStartedByExecution"))
                                                       .expressionAttributeValues(Map.of(":v_executionId",
                                                                                         AttributeValue.builder()
                                                                                                       .s(executionId)
                                                                                                       .build()))
                                                       .conditionExpression(
                                                               conditionExpression)
                                                       .build());

            return true;
        } catch (ConditionalCheckFailedException e) {
            logger.info("Failed to acquire lock for starting ecs instance");
            return false;
        }
    }


    @Override
    public void saveRunnerData(RunnerData runnerData) {

        Map<String, AttributeValueUpdate> map = new HashMap<>();
        map.put("stackName", toStringUpdateAttribute(runnerData.getStackName()));
        map.put("distributionId", toStringUpdateAttribute(runnerData.getDistributionId().asString()));
        map.put("distributionName", toStringUpdateAttribute(runnerData.getDistributionName().asString()));
        map.put("environment", toStringUpdateAttribute(runnerData.getEnvironment().asString()));
        map.put("attiniObjectIdentifier", toStringUpdateAttribute(runnerData.getObjectIdentifier()));
        map.put("runnerName", toStringUpdateAttribute(runnerData.getRunnerName()));
        runnerData.getTaskId().ifPresent(s -> map.put("taskId", toStringUpdateAttribute(s)));
        map.put("taskDefinitionArn", toStringUpdateAttribute(runnerData.getTaskDefinitionArn()));
        map.put("started", AttributeValueUpdate.builder().value(AttributeValue.builder().bool(false).build()).build());
        map.put("taskConfigHashCode",
                toStringUpdateAttribute(String.valueOf(runnerData.getTaskConfiguration().hashCode())));
        map.put("cluster", toStringUpdateAttribute(runnerData.getTaskConfiguration().cluster()));
        map.put("shutdownHookDisabled",
                AttributeValueUpdate.builder()
                                    .value(AttributeValue.builder().bool(runnerData.shutdownHookDisabled()).build())
                                    .build());

        runnerData.getStartedByExecutionArn()
                  .ifPresent(sfnExecutionArn -> map.put("startedByExecutionArn",
                                                        toStringUpdateAttribute(sfnExecutionArn.asString())));
        runnerData.getEc2()
                  .ifPresent(ec2 -> map.put("ec2ConfigHashCode",
                                            toStringUpdateAttribute(String.valueOf(ec2.getConfigHashCode()))));
        runnerData.getEc2()
                  .flatMap(Ec2::getLatestEc2InstanceId)
                  .ifPresent(s -> map.put("latestEc2InstanceId", toStringUpdateAttribute(s)));
        runnerData.getContainer().ifPresent(s -> map.put("container", toStringUpdateAttribute(s)));

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .attributeUpdates(map)
                                                   .key(createDynamoKey(runnerData.getStackName(),
                                                                        runnerData.getRunnerName()))
                                                   .build());
    }

    @Override
    public void saveManualApprovalData(DeploymentPlanExecutionMetadata metadata, DeployOriginData deployOriginData) {

        Map<String, AttributeValueUpdate> map = new HashMap<>();
        map.put("distributionId", toStringUpdateAttribute(deployOriginData.getDistributionId().asString()));
        map.put("distributionName", toStringUpdateAttribute(deployOriginData.getDistributionName().asString()));
        map.put("environment", toStringUpdateAttribute(deployOriginData.getEnvironment().asString()));
        map.put("attiniObjectIdentifier", toStringUpdateAttribute(deployOriginData.getObjectIdentifier().asString()));
        map.put("stepName", toStringUpdateAttribute(metadata.stepName()));
        map.put("sfnToken", toStringUpdateAttribute(metadata.sfnToken()));

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .attributeUpdates(map)
                                                   .key(createManualApprovalDynamoKey(deployOriginData.getEnvironment()
                                                                                                      .asString(),
                                                                                      deployOriginData.getDistributionName()
                                                                                                      .asString(),
                                                                                      metadata.stepName()))
                                                   .build());
    }


    @Override
    public void saveStackData(StackData stackData) {
        saveStackData(stackData, null);
    }


    @Override
    public void saveStackData(StackData stackData, String stackId) {

        logger.info("Updating stack data in in AttiniResourceStates table for stack " + stackData.getStackConfiguration()
                                                                                                 .getStackName());

        Map<String, AttributeValueUpdate> inputMap = new HashMap<>();
        inputMap.put("distributionId", toStringUpdateAttribute(stackData.getDistributionId().asString()));
        inputMap.put("distributionName", toStringUpdateAttribute(stackData.getDistributionName().asString()));
        inputMap.put("environment", toStringUpdateAttribute(stackData.getEnvironment().asString()));
        inputMap.put("desiredState",
                     toStringUpdateAttribute(stackData.getStackConfiguration().getDesiredState().name()));
        inputMap.put("attiniObjectIdentifier", toStringUpdateAttribute(stackData.getObjectIdentifier().asString()));
        inputMap.put("stepName", toStringUpdateAttribute(stackData.getDeploymentPlanExecutionMetadata()
                                                                  .stepName()));
        inputMap.put("stackError", AttributeValueUpdate.builder().action(AttributeAction.DELETE).build());
        inputMap.put("errors",
                     AttributeValueUpdate.builder()
                                         .value(AttributeValue.builder().l(Collections.emptyList()).build())
                                         .build());
        inputMap.put("clientRequestToken",
                     toStringUpdateAttribute(stackData.getClientRequestToken().asString()));
        inputMap.put("stackName", toStringUpdateAttribute(stackData.getStackConfiguration().getStackName()));
        inputMap.put("sfnExecutionArn",
                     toStringUpdateAttribute(stackData.getDeploymentPlanExecutionMetadata().executionArn().asString()));
        inputMap.put("sfnToken", toStringUpdateAttribute(stackData.getDeploymentPlanExecutionMetadata().sfnToken()));

        inputMap.put("template", toStringUpdateAttribute(stackData.getStackConfiguration().getTemplate()));
        inputMap.put("stackType", toStringUpdateAttribute("Cfn"));
        if (stackId != null) {
            inputMap.put("stackId",
                         toStringUpdateAttribute(stackId));
        }

        Map<String, AttributeValueUpdate> finalMap =
                stackData.getStackConfiguration()
                         .getOutputPath()
                         .map(s -> {
                             Map<String, AttributeValueUpdate> map = new HashMap<>(inputMap);
                             map.put("outputPath", toStringUpdateAttribute(s));
                             return map;
                         }).orElse(inputMap);

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .attributeUpdates(finalMap)
                                                   .key(createStackDynamoKey(stackData.getStackConfiguration()))
                                                   .build());


    }

    @Override
    public Optional<SfnExecutionArn> getStacksSfnExecutionArn(StackConfiguration stackConfiguration) {
        Map<String, AttributeValue> item = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                .tableName(environmentVariables.getResourceStatesTableName())
                                                                                .key(createStackDynamoKey(
                                                                                        stackConfiguration))
                                                                                .build()).item();

        if (item.containsKey("sfnExecutionArn")) {
            return Optional.of(SfnExecutionArn.create(item.get("sfnExecutionArn").s()));
        }
        return Optional.empty();
    }

    private AttributeValue toStringAttribute(String value) {
        return AttributeValue.builder()
                             .s(value)
                             .build();
    }

    private AttributeValueUpdate toStringUpdateAttribute(String value) {
        return AttributeValueUpdate.builder().value(toStringAttribute(value)).build();
    }

    @Override
    public void saveStackId(String stackId, StackConfiguration stackConfiguration) {

        logger.info("Setting stack id");

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(createStackDynamoKey(stackConfiguration))
                                                   .attributeUpdates(Map.of("stackId",
                                                                            toStringUpdateAttribute(stackId)))
                                                   .build());
    }

    @Override
    public Optional<StackTemplate> getStackTemplate(StackConfiguration stackConfiguration) {

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                        .tableName(environmentVariables.getResourceStatesTableName())
                                                                        .key(createStackDynamoKey(stackConfiguration))
                                                                        .build());

        if (response.hasItem() && response.item().containsKey("template")) {
            return Optional.of(new StackTemplate(ObjectIdentifier.of(response.item()
                                                                             .get("attiniObjectIdentifier")
                                                                             .s()),
                                                 response.item()
                                                         .get("template")
                                                         .s()));
        }
        return Optional.empty();
    }


    private String getName(StackConfiguration stackConfiguration) {
        return stackConfiguration.getStackName() +
               "-" +
               stackConfiguration.getRegion().orElseGet(environmentVariables::getRegion) +
               "-" +
               stackConfiguration.getExecutionRole().map(s -> s.split(":")[4])
                                 .orElseGet(environmentVariables::getAccountId);
    }


    private Map<String, AttributeValue> createStackDynamoKey(StackConfiguration stackConfiguration) {
        return Map.of("resourceType", toStringAttribute("CloudformationStack"),
                      "name", toStringAttribute(getName(stackConfiguration)));

    }

    private Map<String, AttributeValue> createDynamoKey(String stackName, String runnerName) {
        return Map.of("resourceType", toStringAttribute("Runner"),
                      "name", toStringAttribute(stackName + "-" + runnerName));

    }

    private Map<String, AttributeValue> createManualApprovalDynamoKey(String environment,
                                                                      String distributionName,
                                                                      String stepName) {
        return Map.of("resourceType", toStringAttribute("ManualApproval"),
                      "name", toStringAttribute(environment + "-" + distributionName + "-" + stepName));

    }

}
