
package attini.deploy.origin.deploystack;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.DistributionType;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionContext;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

public class DynamoDeployDataFacade implements DeployDataFacade {

    private final static Logger logger = Logger.getLogger(DeployInitStackService.class);
    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public DynamoDeployDataFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }



    @Override
    public void savePlatformDeployment(SaveDeploymentDataRequest request) {
        save(request, null, DistributionType.PLATFORM, null);
    }

    @Override
    public void saveAppDeployment(SaveDeploymentDataRequest request,
                     String stackName,
                     String sfnArn) {
        save(request, stackName, DistributionType.APP, sfnArn);
    }

    private void save(SaveDeploymentDataRequest request,
                     String stackName,
                     DistributionType distributionType,
                     String sfnArn) {

        logger.info("Saving deployment plan source data");
        Map<String, AttributeValue> deployData = new HashMap<>();
        Map<String, AttributeValue> deploySource = new HashMap<>();

        deploySource.put("deploymentSourceBucket", toAttribute(environmentVariables.getArtifactBucket()));

        request.getDistributionData().ifPresent(distributionData -> {
            deploySource.put("deploymentSourcePrefix", toAttribute(distributionData.getArtifactPath()));
            AttiniConfig attiniConfig = distributionData.getAttiniConfig();
            Map<String, AttributeValue> attiniDistributionTags = attiniConfig.getAttiniDistributionTags()
                                                                             .entrySet()
                                                                             .stream()
                                                                             .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                       entry -> toAttribute(
                                                                                                               entry.getValue())));
            deployData.put("distributionTags", toAttribute(attiniDistributionTags));

            if (stackName == null) {
                attiniConfig.getAttiniInitDeployStackConfig()
                            .ifPresent(attiniInitDeployStackConfig ->
                                               deployData.put("stackName",
                                                              toAttribute(
                                                                      attiniInitDeployStackConfig
                                                                              .getInitDeployStackName())));
            } else {

                AttributeValue attributeValue = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                                     .tableName(environmentVariables.getResourceStatesTableName())
                                                                                     .key(Map.of("resourceType",
                                                                                                 AttributeValue.builder()
                                                                                                               .s("DeploymentPlan")
                                                                                                               .build(),
                                                                                                 "name",
                                                                                                 AttributeValue.builder().s(sfnArn).build()))
                                                                                     .build()).item().get("attiniSteps");

                if (attributeValue != null && attributeValue.hasL()){
                    deployData.put("attiniSteps",attributeValue);
                }
                deployData.put("stackName",
                               toAttribute(stackName));
            }

            attiniConfig.getVersion().ifPresent(version -> deployData.put("version", toAttribute(version.asString())));


        });


        DistributionContext distributionContext = request.getDistributionContext();


        deployData.put("deploymentName",
                       toAttribute(String.format("%s-%s",
                                                 distributionContext.getEnvironment().asString(),
                                                 distributionContext.getDistributionName().asString())));

        deployData.put("distributionName", toAttribute(distributionContext.getDistributionName().asString()));
        deployData.put("environment", toAttribute(distributionContext.getEnvironment().asString()));
        deployData.put("distributionId", toAttribute(distributionContext.getDistributionId().asString()));
        deployData.put("deploymentTime", toAttribute(0));
        deployData.put("objectIdentifier", toAttribute(distributionContext.getObjectIdentifier().asString()));
        deployData.put("executionArns", AttributeValue.builder().m(Collections.emptyMap()).build());
        deployData.put("initStackErrors", AttributeValue.builder().l(Collections.emptyList()).build());
        deployData.put("errors", AttributeValue.builder().m(Collections.emptyMap()).build());
        deployData.put("deploymentSource", toAttribute(deploySource));
        deployData.put("deploymentType",
                       distributionType == DistributionType.APP ? toAttribute("app") : toAttribute("platform"));
        deployData.put("initStackUnchanged", AttributeValue.builder().bool(request.isUnchanged()).build());
        if (distributionType == DistributionType.APP){
            deployData.put("deploymentPlanCount", AttributeValue.builder().n("1").build());

        }
        request.getError().ifPresent(error -> {
            deployData.put("errorMessage", toAttribute(error.getErrorMessage()));
            deployData.put("errorCode", toAttribute(error.getErrorCode()));
        });


        dynamoDbClient.putItem(PutItemRequest.builder()
                                             .tableName(environmentVariables.getDeployDataTableName())
                                             .item(deployData)
                                             .build());

        deployData.replace("deploymentTime", toAttribute(request.getDeployTime()));

        dynamoDbClient.putItem(PutItemRequest.builder()
                                             .tableName(environmentVariables.getDeployDataTableName())
                                             .item(deployData)
                                             .build());
    }

    @Override
    public int countDeployDataAfterDate(DistributionName distributionName, Environment environment, LocalDate from) {

        long toTimeStamp = from.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        QueryRequest request = QueryRequest.builder()
                                           .select(Select.COUNT)
                                           .tableName(environmentVariables.getDeployDataTableName())
                                           .keyConditionExpression(
                                                   "deploymentName=:v_deployName and deploymentTime > :v_from")
                                           .expressionAttributeValues(Map.of(
                                                   ":v_deployName",
                                                   AttributeValue.builder()
                                                                 .s(getDeployName(distributionName, environment))
                                                                 .build(),
                                                   ":v_from",
                                                   AttributeValue.builder()
                                                                 .n(String.valueOf(toTimeStamp))
                                                                 .build()))
                                           .build();

        return dynamoDbClient.queryPaginator(request)
                             .stream()
                             .map(QueryResponse::count)
                             .reduce(0, Integer::sum);
    }

    private static String getDeployName(DistributionName distributionName, Environment environment) {
        return environment.asString() + "-" + distributionName.asString();
    }

    @Override
    public DeployData getLatestDeployData(DistributionName distributionName, Environment environment) {

        GetItemResponse item = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                    .tableName(environmentVariables.getDeployDataTableName())
                                                                    .key(Map.of("deploymentName",
                                                                                toAttribute(getDeployName(
                                                                                        distributionName,
                                                                                        environment)),
                                                                                "deploymentTime",
                                                                                toAttribute(0)))
                                                                    .build());

        return toDeploySource().apply(item.item());
    }

    @Override
    public List<DeployData> getDeployData(DistributionName distributionName, Environment environment, LocalDate to) {

        long toTimeStamp = to.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        QueryRequest queryRequest = QueryRequest.builder()
                                                .tableName(environmentVariables.getDeployDataTableName())
                                                .keyConditionExpression(
                                                        "deploymentName=:v_deployName and deploymentTime < :v_to")
                                                .expressionAttributeValues(Map.of(
                                                        ":v_deployName",
                                                        AttributeValue.builder()
                                                                      .s(getDeployName(distributionName, environment))
                                                                      .build(),
                                                        ":v_to",
                                                        AttributeValue.builder()
                                                                      .n(String.valueOf(toTimeStamp))
                                                                      .build()))
                                                .build();

        return dynamoDbClient.query(queryRequest)
                             .items()
                             .stream()
                             .map(toDeploySource())
                             .collect(toList());
    }

    private Function<Map<String, AttributeValue>, DeployData> toDeploySource() {
        return map -> {
            String distId = map.get("distributionId") == null ? "not_defined" : map.get("distributionId").s();
            return DeployData.builder()
                             .setDeployName(map.get("deploymentName").s())
                             .setDeployTime(Instant.ofEpochMilli(Long.parseLong(map.get(
                                     "deploymentTime").n())))
                             .setDistributionId(DistributionId.of(distId))
                             .setDeploymentSourceBucket(map.get("deploymentSource")
                                                           .m()
                                                           .get("deploymentSourceBucket")
                                                           .s())
                             .setObjectIdentifier(ObjectIdentifier.of(map.get("objectIdentifier").s()))
                             .setErrorMessage(map.containsKey("errorMessage") ? map.get("errorMessage").s() : null)
                             .build();
        };
    }


    private static AttributeValue toAttribute(String value) {
        return AttributeValue.builder()
                             .s(value)
                             .build();
    }

    private static AttributeValue toAttribute(long value) {
        return AttributeValue.builder()
                             .n(String.valueOf(value))
                             .build();
    }

    private static AttributeValue toAttribute(Map<String, AttributeValue> valueMap) {
        return AttributeValue.builder()
                             .m(valueMap)
                             .build();
    }
}
