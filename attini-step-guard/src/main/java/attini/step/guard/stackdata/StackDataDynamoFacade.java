/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import attini.step.guard.AttiniContext;
import attini.step.guard.CloudFormationEvent;
import attini.step.guard.CloudFormationStackDataNotFoundException;
import attini.step.guard.EnvironmentVariables;
import attini.step.guard.InitDeploySnsEvent;
import attini.step.guard.cdk.CdkStack;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class StackDataDynamoFacade implements StackDataFacade {

    private static final Logger logger = Logger.getLogger(StackDataDynamoFacade.class);
    private static final String SFN_ARNS = "sfnArns";
    private static final String RUNNERS = "runners";
    private static final String SFN_TOKEN = "sfnToken";
    private static final String STACK_ID = "stackId";
    private static final String STEP_NAME = "stepName";
    private static final String OUTPUT_PATH = "outputPath";
    private static final String OBJECT_IDENTIFIER = "attiniObjectIdentifier";
    private static final String DISTRIBUTION_ID = "distributionId";
    private static final String DISTRIBUTION_NAME = "distributionName";
    private static final String ENVIRONMENT = "environment";
    private static final String CLIENT_REQUEST_TOKEN = "clientRequestToken";
    private static final String DESIRED_STATE = "desiredState";
    private static final String INIT_TEMPLATE_MD5 = "initStackMd5Hex";


    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;


    public StackDataDynamoFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }


    @Override
    public InitDeployData getInitDeployData(String stackName) {
        return getInitDeployData(stackName, null);
    }

    @Override
    public InitDeployData getInitDeployData(String stackName, String clientRequestToken) {
        GetItemRequest getItemRequest = GetItemRequest.builder()
                                                      .key(createInitStackDataKey(stackName))
                                                      .consistentRead(true)
                                                      .tableName(environmentVariables.getResourceStatesTableName())
                                                      .build();
        GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
        if (getItemResponse.hasItem()) {
            Map<String, AttributeValue> item = getItemResponse.item();

            if (clientRequestToken != null && !DigestUtils.md5Hex(item.get(OBJECT_IDENTIFIER).s())
                                                          .toUpperCase()
                                                          .equals(clientRequestToken)) {
                removeInitTemplateMd5(stackName);
                throw new IllegalStateException(
                        "The object identifier (clientRequestToken) in the the event does not match with the the value stored in the AttiniResourceStates table");
            }

            List<String> runners = item.get(RUNNERS) == null ? Collections.emptyList() : item.get(RUNNERS).ss();
            List<String> sfnArns = item.get(SFN_ARNS) == null ? Collections.emptyList() : item.get(SFN_ARNS).ss();

            return new InitDeployData(sfnArns,
                                      ObjectIdentifier.of(item.get(OBJECT_IDENTIFIER).s()),
                                      Environment.of(item.get(ENVIRONMENT).s()),
                                      DistributionName.of(item.get(DISTRIBUTION_NAME).s()),
                                      runners);

        } else {
            String msg = String.format("Sfn response data for cfn stack %s was not found",
                                       stackName);
            logger.fatal(msg);
            throw new CloudFormationStackDataNotFoundException(msg);
        }
    }

    @Override
    public void removeInitTemplateMd5(String stackName) {
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .key(createInitStackDataKey(stackName))
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .attributeUpdates(Map.of(INIT_TEMPLATE_MD5,
                                                                            AttributeValueUpdate.builder()
                                                                                                .value(AttributeValue.builder()
                                                                                                                     .s("")
                                                                                                                     .build())
                                                                                                .build()))
                                                   .build());
    }

    @Override
    public StackData getStackData(CloudFormationEvent cloudFormationEvent) {

        GetItemRequest getItemRequest = GetItemRequest.builder()
                                                      .key(createKey(cloudFormationEvent))
                                                      .tableName(environmentVariables.getResourceStatesTableName())
                                                      .build();
        GetItemResponse getItemResponse = dynamoDbClient.getItem(getItemRequest);
        if (getItemResponse.hasItem()) {

            Map<String, AttributeValue> item = getItemResponse.item();
            if (!item.get(CLIENT_REQUEST_TOKEN).s().equals(cloudFormationEvent.getClientRequestToken())) {
                throw new IllegalStateException(
                        "The object identifier (clientRequestToken) in the the event does not match with the the value stored in the AttiniResourceStates table");
            }

            return StackData.builder()
                            .setSfnToken(item.containsKey(SFN_TOKEN) ? item.get(SFN_TOKEN).s() : null)
                            .setStackId(item.containsKey(STACK_ID) ? item.get(STACK_ID).s() : null)
                            .setStepName(item.containsKey(STEP_NAME) ? item.get(STEP_NAME).s() : null)
                            .setOutputPath(item.containsKey(OUTPUT_PATH) ? item.get(OUTPUT_PATH).s() : null)
                            .setEnvironment(item.containsKey(ENVIRONMENT) ? Environment.of(item.get(ENVIRONMENT)
                                                                                               .s()) : null)
                            .setDistributionId(item.containsKey(DISTRIBUTION_ID) ? DistributionId.of(item.get(
                                    DISTRIBUTION_ID).s()) : null)
                            .setDistributionName(item.containsKey(DISTRIBUTION_NAME) ? DistributionName.of(item.get(
                                                                                                                       DISTRIBUTION_NAME)
                                                                                                               .s()) : null)
                            .setObjectIdentifier(item.containsKey(OBJECT_IDENTIFIER) ? ObjectIdentifier.of(item.get(
                                                                                                                       OBJECT_IDENTIFIER)
                                                                                                               .s()) : null)
                            .setDesiredState(item.containsKey(DESIRED_STATE) ? toDesiredState(item.get(DESIRED_STATE)
                                                                                                  .s()) : null)
                            .build();

        } else {
            String msg = String.format("Sfn response data for cfn stack %s was not found",
                                       cloudFormationEvent.getStackName());
            logger.fatal(msg);
            throw new CloudFormationStackDataNotFoundException(msg);
        }
    }

    private static DesiredState toDesiredState(String desiredState) {
        if (desiredState.equals("DELETED")) {
            return DesiredState.DELETED;
        }
        return DesiredState.DEPLOYED;
    }

    @Override
    public void deleteCfnStack(CloudFormationEvent cloudFormationEvent) {
        deleteItem(DeleteItemRequest.builder()
                                    .tableName(environmentVariables.getResourceStatesTableName())
                                    .key(createKey(cloudFormationEvent))
                                    .build());
    }

    @Override
    public void deleteInitDeploy(InitDeploySnsEvent stepGuardInput) {

        logger.info("Deleting init deploy data");

        InitDeployData initDeployData = getInitDeployData(stepGuardInput.getStackName());
        initDeployData.getSfnArns()
                      .stream()
                      .map(s -> DeleteItemRequest.builder()
                                                 .tableName(environmentVariables.getResourceStatesTableName())
                                                 .key(Map.of("resourceType",
                                                             AttributeValue.builder()
                                                                           .s("DeploymentPlan")
                                                                           .build(),
                                                             "name",
                                                             AttributeValue.builder()
                                                                           .s(s)
                                                                           .build()))
                                                 .build())
                      .forEach(this::deleteItem);

        initDeployData.getRunners()
                      .stream()
                      .map(s -> DeleteItemRequest.builder()
                                                 .tableName(environmentVariables.getResourceStatesTableName())
                                                 .key(Map.of("resourceType",
                                                             AttributeValue.builder()
                                                                           .s("Runner")
                                                                           .build(),
                                                             "name",
                                                             AttributeValue.builder()
                                                                           .s(stepGuardInput.getStackName() + "-" + s)
                                                                           .build()))
                                                 .build())
                      .forEach(this::deleteItem);


        deleteItem(DeleteItemRequest.builder()
                                    .tableName(
                                            environmentVariables.getResourceStatesTableName())
                                    .key(createInitStackDataKey(stepGuardInput.getStackName()))
                                    .build());
    }

    private void deleteItem(DeleteItemRequest deleteItemRequest) {
        try {
            dynamoDbClient.deleteItem(deleteItemRequest);
        } catch (ResourceNotFoundException e) {
            logger.info("Could not delete resource because it does not exist, key =" + deleteItemRequest.key());
        }
    }

    @Override
    public void saveError(CloudFormationEvent cloudFormationEvent, String error) {
        saveError(createKey(cloudFormationEvent), cloudFormationEvent, error);
    }


    @Override
    public void saveInitDeployError(InitDeploySnsEvent cloudFormationEvent, String error) {
        saveError(createInitStackDataKey(cloudFormationEvent.getStackName()), cloudFormationEvent, error);
    }

    private void saveError(Map<String, AttributeValue> key, CloudFormationEvent cloudFormationEvent, String error) {
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

        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(key)
                                                   .updateExpression("SET errors = list_append(errors, :val)")
                                                   .expressionAttributeValues(Map.of(":val", list))
                                                   .build());
    }

    @Override
    public void saveCdkStack(AttiniContext attiniContext, String stepName, CdkStack cdkStack) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                                             .tableName(environmentVariables.getResourceStatesTableName())
                                             .item(Map.of("resourceType",
                                                          stringAttribute("CdkStack"),
                                                          "name",
                                                          stringAttribute("%s-%s-%s" .formatted(cdkStack.name(),
                                                                                                cdkStack.environment()
                                                                                                        .region(),
                                                                                                cdkStack.environment()
                                                                                                        .account())),
                                                          STEP_NAME,
                                                          stringAttribute(stepName),
                                                          DISTRIBUTION_NAME,
                                                          stringAttribute(attiniContext.getDistributionName()
                                                                                       .asString()),
                                                          DISTRIBUTION_ID,
                                                          stringAttribute(attiniContext.getDistributionId().asString()),
                                                          OBJECT_IDENTIFIER, stringAttribute(attiniContext.getObjectIdentifier().asString()),
                                                          ENVIRONMENT, stringAttribute(attiniContext.getEnvironment().asString())
                                             ))
                                             .build());
    }

    private static AttributeValue stringAttribute(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private HashMap<String, AttributeValue> createInitStackDataKey(String stackName) {
        HashMap<String, AttributeValue> key = new HashMap<>();

        key.put("resourceType", AttributeValue.builder()
                                              .s("InitDeployCloudformationStack")
                                              .build());
        key.put("name", AttributeValue.builder()
                                      .s(stackName)
                                      .build());
        return key;
    }

    private String getName(CloudFormationEvent cloudFormationEvent) {
        return cloudFormationEvent.getStackName() +
               "-" +
               cloudFormationEvent.getRegion().orElseGet(environmentVariables::getRegion) +
               "-" +
               cloudFormationEvent.getExecutionRoleArn().map(s -> s.split(":")[4])
                                  .orElseGet(environmentVariables::getAccountId);

    }

    private HashMap<String, AttributeValue> createKey(CloudFormationEvent cloudFormationEvent) {
        HashMap<String, AttributeValue> key = new HashMap<>();

        key.put("resourceType", AttributeValue.builder()
                                              .s("CloudformationStack")
                                              .build());
        key.put("name", AttributeValue.builder()
                                      .s(getName(cloudFormationEvent))
                                      .build());
        return key;
    }
}
