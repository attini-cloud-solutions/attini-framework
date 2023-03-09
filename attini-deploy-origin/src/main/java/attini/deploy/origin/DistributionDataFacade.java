package attini.deploy.origin;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.Version;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class DistributionDataFacade {

    private static final Logger logger = Logger.getLogger(DistributionDataFacade.class);

    private final DynamoDbClient dynamoDbClient;
    private final EnvironmentVariables environmentVariables;

    public DistributionDataFacade(DynamoDbClient dynamoDbClient, EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public void saveDistributionData(AttiniConfig attiniConfig,
                                     InitDeployEvent initDeployEvent,
                                     String deploymentSourcePrefix) {

        logger.info("Saving distribution data for distribution " + attiniConfig.getAttiniDistributionName().asString());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("resourceType", AttributeValue.builder().s("Distribution").build());
        item.put("name",
                 AttributeValue.builder()
                               .s(initDeployEvent.getEnvironmentName()
                                                 .asString() + "-" + attiniConfig.getAttiniDistributionName()
                                                                                 .asString())
                               .build());
        item.put("attiniObjectIdentifier",
                 AttributeValue.builder().s(initDeployEvent.getObjectIdentifier().asString()).build());
        item.put("distributionId",
                 AttributeValue.builder().s(attiniConfig.getAttiniDistributionId().asString()).build());
        item.put("distributionName",
                 AttributeValue.builder().s(attiniConfig.getAttiniDistributionName().asString()).build());
        item.put("environment", AttributeValue.builder().s(initDeployEvent.getEnvironmentName().asString()).build());
        item.put("userIdentity", AttributeValue.builder().s(initDeployEvent.getUserIdentity()).build());
        item.put("sourceS3Bucket", AttributeValue.builder().s(initDeployEvent.getS3Bucket()).build());
        item.put("sourceS3key", AttributeValue.builder().s(initDeployEvent.getS3Key()).build());
        item.put("deploymentSourcePrefix", AttributeValue.builder().s(deploymentSourcePrefix).build());
        attiniConfig.getVersion()
                    .ifPresent(version -> item.put("version", AttributeValue.builder().s(version.asString()).build()));

        item.put("distributionTags",
                 AttributeValue.builder()
                               .m(attiniConfig.getAttiniDistributionTags().entrySet()
                                              .stream()
                                              .collect(toMap(Map.Entry::getKey,
                                                             o -> AttributeValue.builder().s(o.getValue()).build())))
                               .build());


        if (!attiniConfig.getDependencies().isEmpty()) {
            item.put("dependencies",
                     AttributeValue.builder()
                                   .l(attiniConfig.getDependencies()
                                                  .stream()
                                                  .map(dependency -> AttributeValue.builder()
                                                                                   .m(Map.of("distributionName",
                                                                                             AttributeValue.builder()
                                                                                                           .s(dependency.distributionName()
                                                                                                                        .asString())
                                                                                                           .build()))
                                                                                   .build())
                                                  .collect(Collectors.toList()))
                                   .build());
        }
        attiniConfig.getAttiniInitDeployStackConfig()
                    .ifPresent(initDeployStackConfig -> item.put("initStackName",
                                                                 AttributeValue.builder()
                                                                               .s(initDeployStackConfig.getInitDeployStackName())
                                                                               .build()));

        dynamoDbClient.putItem(PutItemRequest.builder()
                                             .tableName(environmentVariables.getResourceStatesTableName())
                                             .item(item)
                                             .build());

    }

    public Optional<Distribution> getDistribution(DistributionName distName, Environment environment) {

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                                                                        .tableName(environmentVariables.getResourceStatesTableName())
                                                                        .key(Map.of("resourceType",
                                                                                    AttributeValue.builder()
                                                                                                  .s("Distribution")
                                                                                                  .build(),
                                                                                    "name",
                                                                                    AttributeValue.builder()
                                                                                                  .s(environment.asString() + "-" + distName.asString())
                                                                                                  .build()))
                                                                        .build());
        if (response.hasItem()) {
            String outputUrl = response.item().containsKey("outputUrl") ? response.item()
                                                                                  .get("outputUrl")
                                                                                  .s() : null;
            Version version = response.item().containsKey("version") ? Version.of(response.item()
                                                                                          .get("version")
                                                                                          .s()) : null;
            return Optional.of(new Distribution(DistributionName.of(response.item().get("distributionName").s()),
                                                DistributionId.of(response.item().get("distributionId").s()),
                                                outputUrl,
                                                version));
        }

        return Optional.empty();
    }

}
