/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin.deploystack;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import attini.deploy.origin.DistributionData;
import attini.deploy.origin.config.InitDeployStackConfig;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class DynamoInitDeployStackFacade {

    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public DynamoInitDeployStackFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public Optional<String> geInitStackMd5(String stackName) {
        AttributeValue hexAttribute =
                dynamoDbClient.getItem(GetItemRequest.builder()
                                                     .tableName(environmentVariables.getResourceStatesTableName())
                                                     .key(Map.of("resourceType",
                                                                 AttributeValue.builder()
                                                                               .s("InitDeployCloudformationStack")
                                                                               .build(),
                                                                 "name",
                                                                 AttributeValue.builder()
                                                                               .s(stackName)
                                                                               .build()))
                                                     .build())
                              .item()
                              .get("initStackMd5Hex");

        return hexAttribute == null ? Optional.empty() : Optional.of(hexAttribute.s());
    }

    public void setInitDeployError(InitDeployStackConfig initDeployStackConfig,
                                   String error) {

        Map<String, AttributeValueUpdate> item = new HashMap<>();

        item.put("errors",
                 AttributeValueUpdate.builder()
                                     .value(AttributeValue.builder()
                                                          .l(Collections.singletonList(AttributeValue.builder()
                                                                                                     .s(error)
                                                                                                     .build()))
                                                          .build())
                                     .build());

        item.put("initStackUnchanged",
                 AttributeValueUpdate.builder().value(AttributeValue.builder().bool(true).build()).build());
        item.put("initStackMd5Hex",
                 AttributeValueUpdate.builder()
                                     .action(AttributeAction.DELETE)
                                     .build());

        Map<String, AttributeValue> key = Map.of("resourceType",
                                                 AttributeValue.builder()
                                                               .s("InitDeployCloudformationStack")
                                                               .build(),
                                                 "name",
                                                 AttributeValue.builder()
                                                               .s(initDeployStackConfig.getInitDeployStackName())
                                                               .build());
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(key)
                                                   .attributeUpdates(item)
                                                   .build());

    }

    public void updateInitDeployItemForUnchangedStack(InitDeployStackConfig initDeployStackConfig,
                                                      DistributionContext distributionContext,
                                                      DistributionData distributionData) {

        Map<String, AttributeValueUpdate> item = new HashMap<>();
        item.put("attiniObjectIdentifier",
                 toUpdateStringAttribute(distributionContext.getObjectIdentifier().asString()));
        item.put("distributionId", toUpdateStringAttribute(distributionContext.getDistributionId().asString()));
        item.put("distributionName", toUpdateStringAttribute(distributionContext.getDistributionName().asString()));
        item.put("environment", toUpdateStringAttribute(distributionContext.getEnvironment().asString()));
        item.put("errors", AttributeValueUpdate.builder()
                                               .value(AttributeValue.builder()
                                                                    .l(Collections.emptyList())
                                                                    .build())
                                               .build());

        Map<String, AttributeValue> variables =
                initDeployStackConfig.getVariables(distributionContext.getEnvironment())
                                     .entrySet()
                                     .stream()
                                     .collect(toMap(Entry::getKey,
                                                    entry -> AttributeValue.builder()
                                                                           .s(entry.getValue())
                                                                           .build()));
        item.put("variables", AttributeValueUpdate.builder()
                                                  .value(AttributeValue.builder()
                                                                       .m(variables)
                                                                       .build())
                                                  .build());
        item.put("initStackUnchanged",
                 AttributeValueUpdate.builder()
                                     .value(AttributeValue.builder()
                                                          .bool(true)
                                                          .build())
                                     .build());


        distributionData.getTemplateMd5Hex()
                        .ifPresent(s -> item.put("initStackMd5Hex",
                                                 AttributeValueUpdate.builder()
                                                                     .value(AttributeValue.builder().s(s).build())
                                                                     .build()));

        Map<String, AttributeValue> key = Map.of("resourceType",
                                                 AttributeValue.builder()
                                                               .s("InitDeployCloudformationStack")
                                                               .build(),
                                                 "name",
                                                 AttributeValue.builder()
                                                               .s(initDeployStackConfig.getInitDeployStackName())
                                                               .build());
        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .key(key)
                                                   .attributeUpdates(item)
                                                   .build());

        AttributeValue sfnArns = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                      .tableName(environmentVariables.getResourceStatesTableName())
                                                                      .key(key)
                                                                      .projectionExpression("sfnArns")
                                                                      .build())
                                               .item()
                                               .get("sfnArns");

        //Update the deployment plan state in case there are no changes to the init stack and the custom resource is not triggered
        if (sfnArns != null) {
            sfnArns.ss()
                   .forEach(s -> updateDeploymentPlanState(s,
                                                           distributionContext.getObjectIdentifier().asString(),
                                                           distributionContext.getDistributionId().asString()));
        }


    }

    private static AttributeValueUpdate toUpdateStringAttribute(String value) {
        return AttributeValueUpdate.builder()
                                   .value(toStringAttribute(value))
                                   .build();
    }


    private static AttributeValue toStringAttribute(String value) {
        return AttributeValue.builder()
                             .s(value)
                             .build();
    }

    private void updateDeploymentPlanState(String sfnArn, String objectIdentifier, String distributionId) {
        Map<String, AttributeValueUpdate> item = new HashMap<>();
        item.put("attiniObjectIdentifier",
                 AttributeValueUpdate.builder().value(AttributeValue.builder().s(objectIdentifier).build()).build());
        item.put("distributionId",
                 AttributeValueUpdate.builder().value(AttributeValue.builder().s(distributionId).build()).build());


        dynamoDbClient.updateItem(UpdateItemRequest.builder()
                                                   .tableName(environmentVariables.getResourceStatesTableName())
                                                   .attributeUpdates(item)
                                                   .key(Map.of("resourceType",
                                                               AttributeValue.builder().s("DeploymentPlan").build(),
                                                               "name",
                                                               AttributeValue.builder().s(sfnArn).build())
                                                   )
                                                   .build());
    }

    public void saveInitDeployItem(InitDeployStackConfig initDeployStackConfig,
                                   DistributionContext distributionContext,
                                   DistributionData distributionData) {

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("resourceType", AttributeValue.builder().s("InitDeployCloudformationStack").build());
        item.put("name", AttributeValue.builder().s(initDeployStackConfig.getInitDeployStackName()).build());
        item.put("attiniObjectIdentifier",
                 AttributeValue.builder().s(distributionContext.getObjectIdentifier().asString()).build());
        item.put("distributionId",
                 AttributeValue.builder().s(distributionContext.getDistributionId().asString()).build());
        item.put("distributionName",
                 AttributeValue.builder().s(distributionContext.getDistributionName().asString()).build());
        item.put("environment", AttributeValue.builder().s(distributionContext.getEnvironment().asString()).build());
        item.put("errors", AttributeValue.builder().l(Collections.emptyList()).build());
        item.put("variables", AttributeValue.builder()
                                            .m(initDeployStackConfig.getVariables(distributionContext.getEnvironment())
                                                                    .entrySet()
                                                                    .stream()
                                                                    .collect(toMap(Entry::getKey,
                                                                                   entry -> AttributeValue.builder()
                                                                                                          .s(entry.getValue())
                                                                                                          .build())))
                                            .build());


        distributionData.getTemplateMd5Hex()
                        .ifPresent(s -> item.put("initStackMd5Hex", AttributeValue.builder().s(s).build()));

        dynamoDbClient.putItem(PutItemRequest.builder()
                                             .tableName(environmentVariables.getResourceStatesTableName())
                                             .item(item)
                                             .build());

    }
}
