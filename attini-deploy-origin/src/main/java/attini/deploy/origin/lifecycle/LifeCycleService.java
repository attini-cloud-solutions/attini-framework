package attini.deploy.origin.lifecycle;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.logging.Logger;

import attini.deploy.origin.deploystack.DeployData;
import attini.deploy.origin.deploystack.DeployDataFacade;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.deploy.origin.system.IllegalEnvironmentVariableException;
import attini.domain.DistributionName;
import attini.domain.Environment;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Object;

public class LifeCycleService {

    private final static Logger logger = Logger.getLogger(LifeCycleService.class);


    private final EnvironmentVariables environmentVariables;
    private final DeployDataFacade deployDataFacade;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final S3AsyncClient s3AsyncClient;

    public LifeCycleService(EnvironmentVariables environmentVariables,
                            DeployDataFacade deployDataFacade,
                            DynamoDbAsyncClient dynamoDbAsyncClient,
                            S3AsyncClient s3AsyncClient) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.deployDataFacade = requireNonNull(deployDataFacade, "deployDataFacade");
        this.dynamoDbAsyncClient = requireNonNull(dynamoDbAsyncClient, "dynamoDbAsyncClient");
        this.s3AsyncClient = requireNonNull(s3AsyncClient, "s3AsyncClient");
    }

    public CompletableFuture<Void> cleanup(DistributionName distributionName, Environment environment, String deployOriginBucket) {


        try {
            int retainDistributionVersions = environmentVariables.getRetainDistributionVersions();
            int retainDistributionDays = environmentVariables.getRetainDistributionDays();

            if (retainDistributionDays == 0 && retainDistributionVersions == 0) {
                logger.info("Life cycle management is not configured, skipping");
                return CompletableFuture.allOf();
            }

            LocalDate toDate = LocalDate.now().minusDays(retainDistributionDays);

            logger.info("Life cycle configured to remove artifact before " + toDate.format(DateTimeFormatter.ISO_DATE));

            int count = deployDataFacade.countDeployDataAfterDate(distributionName, environment, toDate);

            logger.info("Nr of deploys after cut off date = " + count);

            int skipVersions = count < retainDistributionVersions ? retainDistributionVersions - count : 0;

            logger.info("Will skip " + skipVersions + " versions");


            List<DeployData> deployDataList = deployDataFacade.getDeployData(distributionName, environment, toDate)
                                                              .stream()
                                                              .filter(deployData -> deployData.getDeployTime()
                                                                                              .toEpochMilli() != 0)
                                                              .sorted((o1, o2) -> o2.getDeployTime()
                                                                                    .compareTo(o1.getDeployTime()))
                                                              .skip(skipVersions).toList();


            logger.info("Will remove " + deployDataList.size() + " distributions");

            return CompletableFuture.allOf(deployDataList.stream()
                                                         .map(deployData -> delete(deployData,
                                                                                   environment,
                                                                                   distributionName,
                                                                                   deployOriginBucket))
                                                         .toArray(CompletableFuture[]::new));
        } catch (IllegalEnvironmentVariableException e) {
            logger.error("Could not perform life cycle management due to illegal environmental variables", e);
            return CompletableFuture.allOf();
        } catch (Exception e) {
            logger.error(
                    "There was an exception during life cycle management, this should be investigated but the deploy will continue.",
                    e);
            return CompletableFuture.allOf();
        }
    }


    private CompletableFuture<Void> delete(DeployData deployData,
                                           Environment environment,
                                           DistributionName distributionName,
                                           String deployOriginBucket) {

        String prefix = environment.asString() + "/" + distributionName.asString() + "/" + deployData.getDistributionId().asString();

        logger.info("removing with prefix " + prefix);

        return s3AsyncClient.listObjects(ListObjectsRequest.builder()
                                                           .bucket(deployData.getDeploymentSourceBucket())
                                                           .prefix(prefix)
                                                           .build())
                            .handleAsync(handleListObjectsError())
                            .thenComposeAsync(deleteObjectsFromDeploymentSource(deployData))
                            .thenComposeAsync(deleteDeploymentOriginVersion(deployData, deployOriginBucket))
                            .handleAsync(handleDeleteObjectError())
                            .thenComposeAsync(deleteDynamoObject(deployData))
                            .thenComposeAsync(deleteItemResponse -> deleteAttiniS3Resources(deployData,
                                                                                            environment,
                                                                                            distributionName))
                            .thenAcceptAsync(unused -> logger.info("deleted distribution id = " + deployData.getDistributionId().asString()));

    }

    private CompletableFuture<Void> deleteAttiniS3Resources(DeployData deployData,
                                                            Environment environment,
                                                            DistributionName distributionName) {

        String prefix = "attini/deployment/" + environment.asString() + "/" + distributionName.asString() + "/" + deployData.getDistributionId().asString();

        logger.info("removing with prefix " + prefix);

        return s3AsyncClient.listObjectVersions(ListObjectVersionsRequest.builder()
                                                                         .bucket(deployData.getDeploymentSourceBucket())
                                                                         .prefix(prefix)
                                                                         .build())
                            .handleAsync(handleListObjectVersionsError())
                            .thenComposeAsync(deleteAttiniResourceObjects(deployData));
    }


    private Function<CompletableFuture<Void>, CompletionStage<DeleteItemResponse>> deleteDynamoObject(
            DeployData deployData) {
        return response -> dynamoDbAsyncClient.deleteItem(DeleteItemRequest.builder()
                                                                           .tableName(
                                                                                   environmentVariables
                                                                                           .getDeployDataTableName())
                                                                           .key(createKey(
                                                                                   deployData))
                                                                           .build());
    }


    private BiFunction<DeleteObjectResponse, Throwable, CompletableFuture<Void>> handleDeleteObjectError() {
        return (deleteObjectResponse, throwable) -> {
            if (throwable != null) {
                logger.warn("Could not delete deploymentOrigin version, will continue with execution chain", throwable);
            }
            return CompletableFuture.allOf();
        };
    }

    private Function<Void, CompletionStage<DeleteObjectResponse>> deleteDeploymentOriginVersion(DeployData deployData,
                                                                                                String deployOriginBucket) {
        return deleteItemResponse -> {
            String[] split = deployData.getObjectIdentifier().asString().split("#");
            return s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                                                                 .key(split[0])
                                                                 .versionId(split[1])
                                                                 .bucket(deployOriginBucket)
                                                                 .build());
        };
    }

    private Function<List<String>, CompletionStage<Void>> deleteObjectsFromDeploymentSource(DeployData deployData) {
        return response -> {
            if (response.isEmpty()) {
                return CompletableFuture.allOf();
            }

            return s3AsyncClient.deleteObjects(DeleteObjectsRequest.builder()
                                                                   .bucket(deployData
                                                                                   .getDeploymentSourceBucket())
                                                                   .delete(toDeleteRequest(response))
                                                                   .build())
                                .thenApply(response1 -> null);
        };
    }

    private Function<List<ObjectVersion>, CompletionStage<Void>> deleteAttiniResourceObjects(DeployData deployData) {
        return response -> {
            if (response.isEmpty()) {
                return CompletableFuture.allOf();
            }

            return s3AsyncClient.deleteObjects(DeleteObjectsRequest.builder()
                                                                   .bucket(deployData
                                                                                   .getDeploymentSourceBucket())
                                                                   .delete(toDeleteVersionRequest(response))
                                                                   .build())
                                .thenApply(response1 -> null);
        };
    }

    private BiFunction<ListObjectsResponse, Throwable, List<String>> handleListObjectsError() {
        return (response, throwable) -> {
            if (throwable != null) {
                logger.warn("Could not list objects, will continue with execution chain", throwable);
                return Collections.emptyList();
            } else {
                return response.contents().stream().map(S3Object::key).collect(toList());
            }
        };
    }

    private BiFunction<ListObjectVersionsResponse, Throwable, List<ObjectVersion>> handleListObjectVersionsError() {
        return (response, throwable) -> {
            if (throwable != null) {
                logger.warn("Could not list objects, will continue with execution chain", throwable);
                return Collections.emptyList();
            } else {
                return response.versions();
            }
        };
    }

    private Delete toDeleteRequest(List<String> key) {
        return Delete.builder()
                     .objects(
                             key.stream()
                                .map(s -> ObjectIdentifier.builder().key(s).build())
                                .collect(toList()))
                     .build();
    }

    private Delete toDeleteVersionRequest(List<ObjectVersion> key) {
        return Delete.builder()
                     .objects(
                             key.stream()
                                .map(objectVersion -> ObjectIdentifier.builder()
                                                                      .key(objectVersion.key())
                                                                      .versionId(objectVersion.versionId())
                                                                      .build())
                                .collect(toList()))
                     .build();
    }

    private static Map<String, AttributeValue> createKey(DeployData deployData) {
        return Map.of(
                "deploymentName",
                AttributeValue.builder().s(deployData.getDeployName()).build(),
                "deploymentTime",
                AttributeValue.builder().n(String.valueOf(deployData.getDeployTime().toEpochMilli())).build());
    }

}
