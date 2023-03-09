package attini.deploy.origin.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.deploy.origin.deploystack.DeployData;
import attini.deploy.origin.deploystack.DeployDataFacade;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectVersionsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.ObjectVersion;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class LifeCycleServiceTest {

    private final static DistributionName DISTRIBUTION_NAME = DistributionName.of("infraTest");
    private final static Environment ENVIRONMENT = Environment.of("dev");
    private final static String DEPLOYMENT_ORIGIN_BUCKET = "my-bucket";


    @Mock
    EnvironmentVariables environmentVariables;
    @Mock
    DeployDataFacade deployDataFacade;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    S3AsyncClient s3AsyncClient;

    LifeCycleService lifeCycleService;

    @BeforeEach
    void setUp() {
        lifeCycleService = new LifeCycleService(environmentVariables,
                                                deployDataFacade,
                                                dynamoDbAsyncClient,
                                                s3AsyncClient);
    }

    @Test
    void shouldDeleteAllArtifacts() throws ExecutionException, InterruptedException {
        when(environmentVariables.getRetainDistributionDays()).thenReturn(10);
        when(environmentVariables.getRetainDistributionVersions()).thenReturn(10);
        LocalDate toDate = LocalDate.now().minusDays(10);
        when(deployDataFacade.countDeployDataAfterDate(DISTRIBUTION_NAME, ENVIRONMENT, toDate)).thenReturn(10);
        when(deployDataFacade.getDeployData(DISTRIBUTION_NAME, ENVIRONMENT, toDate))
                .thenReturn(List.of(DeployData.builder().setDistributionId(DistributionId.of("1222-1223"))
                                              .setObjectIdentifier(ObjectIdentifier.of("test/test.zip#12221-1222"))
                                              .setDeployTime(LocalDate.now()
                                                                      .minusDays(20)
                                                                      .atStartOfDay()
                                                                      .atZone(ZoneId.systemDefault())
                                                                      .toInstant())
                                              .setDeploymentSourceBucket("a-bucket")
                                              .setDeployName("dev-IinfraTest").build()));
        when(s3AsyncClient.listObjects(any(ListObjectsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> ListObjectsResponse.builder().contents(List.of(S3Object.builder().key("test/test/artifact").build())).build()));
        when(s3AsyncClient.listObjectVersions(any(ListObjectVersionsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> ListObjectVersionsResponse.builder().versions(List.of(
                        ObjectVersion.builder().key("attini/test/artifact").versionId("123234").build())).build()));

        when(s3AsyncClient.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteObjectsResponse.builder().build()));
        when(s3AsyncClient.deleteObject(any(DeleteObjectRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteObjectResponse
                .builder().build()));
        when(dynamoDbAsyncClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteItemResponse.builder().build()));


        CompletableFuture<Void> cleanup = lifeCycleService.cleanup(DISTRIBUTION_NAME,
                                                                   ENVIRONMENT,
                                                                   DEPLOYMENT_ORIGIN_BUCKET);


        cleanup.get();
        verify(s3AsyncClient).listObjects(any(ListObjectsRequest.class));
        verify(s3AsyncClient, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3AsyncClient).deleteObject(any(DeleteObjectRequest.class));
        verify(dynamoDbAsyncClient).deleteItem(any(DeleteItemRequest.class));

    }

    @Test
    void shouldContinueIfListFails() throws ExecutionException, InterruptedException {
        when(environmentVariables.getRetainDistributionDays()).thenReturn(10);
        when(environmentVariables.getRetainDistributionVersions()).thenReturn(10);
        LocalDate toDate = LocalDate.now().minusDays(10);
        when(deployDataFacade.countDeployDataAfterDate(DISTRIBUTION_NAME, ENVIRONMENT, toDate)).thenReturn(10);
        when(deployDataFacade.getDeployData(DISTRIBUTION_NAME, ENVIRONMENT, toDate))
                .thenReturn(List.of(DeployData.builder().setDistributionId(DistributionId.of("1222-1223"))
                                              .setObjectIdentifier(ObjectIdentifier.of("test/test.zip#12221-1222"))
                                              .setDeployTime(LocalDate.now()
                                                                      .minusDays(20)
                                                                      .atStartOfDay()
                                                                      .atZone(ZoneId.systemDefault())
                                                                      .toInstant())
                                              .setDeploymentSourceBucket("a-bucket")
                                              .setDeployName("dev-IinfraTest").build()));
        when(s3AsyncClient.listObjects(any(ListObjectsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> {
                    throw new RuntimeException();
                }));

        when(s3AsyncClient.deleteObject(any(DeleteObjectRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteObjectResponse
                .builder().build()));
        when(dynamoDbAsyncClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteItemResponse.builder().build()));



        when(s3AsyncClient.listObjectVersions(any(ListObjectVersionsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> ListObjectVersionsResponse.builder().versions(
                        Collections.emptyList()).build()));

        CompletableFuture<Void> cleanup = lifeCycleService.cleanup(DISTRIBUTION_NAME,
                                                                   ENVIRONMENT,
                                                                   DEPLOYMENT_ORIGIN_BUCKET);

        cleanup.get();
        verify(s3AsyncClient).listObjects(any(ListObjectsRequest.class));
        verify(s3AsyncClient, never()).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3AsyncClient).deleteObject(any(DeleteObjectRequest.class));
        verify(dynamoDbAsyncClient).deleteItem(any(DeleteItemRequest.class));

    }

    @Test
    void shouldContinueIfDeleteVersionFails() throws ExecutionException, InterruptedException {
        when(environmentVariables.getRetainDistributionDays()).thenReturn(10);
        when(environmentVariables.getRetainDistributionVersions()).thenReturn(10);
        LocalDate toDate = LocalDate.now().minusDays(10);
        when(deployDataFacade.countDeployDataAfterDate(DISTRIBUTION_NAME, ENVIRONMENT, toDate)).thenReturn(10);
        when(deployDataFacade.getDeployData(DISTRIBUTION_NAME, ENVIRONMENT, toDate))
                .thenReturn(List.of(DeployData.builder().setDistributionId(DistributionId.of("1222-1223"))
                                              .setObjectIdentifier(ObjectIdentifier.of("test/test.zip#12221-1222"))
                                              .setDeployTime(LocalDate.now()
                                                                      .minusDays(20)
                                                                      .atStartOfDay()
                                                                      .atZone(ZoneId.systemDefault())
                                                                      .toInstant())
                                              .setDeploymentSourceBucket("a-bucket")
                                              .setDeployName("dev-IinfraTest").build()));
        when(s3AsyncClient.listObjects(any(ListObjectsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> ListObjectsResponse.builder().contents(List.of(S3Object.builder().key("test/test/artifact").build())).build()));

        when(s3AsyncClient.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteObjectsResponse.builder().build()));
        when(s3AsyncClient.deleteObject(any(DeleteObjectRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException();
        }));
        when(dynamoDbAsyncClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(CompletableFuture.supplyAsync(() -> DeleteItemResponse.builder().build()));



        when(s3AsyncClient.listObjectVersions(any(ListObjectVersionsRequest.class)))
                .thenReturn(CompletableFuture.supplyAsync(() -> ListObjectVersionsResponse.builder().versions(List.of(
                        ObjectVersion.builder().key("attini/test/artifact").versionId("123234").build())).build()));

        CompletableFuture<Void> cleanup = lifeCycleService.cleanup(DISTRIBUTION_NAME,
                                                                   ENVIRONMENT,
                                                                   DEPLOYMENT_ORIGIN_BUCKET);

        cleanup.get();
        verify(s3AsyncClient).listObjects(any(ListObjectsRequest.class));
        verify(s3AsyncClient, times(2)).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3AsyncClient).deleteObject(any(DeleteObjectRequest.class));
        verify(dynamoDbAsyncClient).deleteItem(any(DeleteItemRequest.class));

    }

    @Test
    void shouldSkipIfConfiguredToZero() throws ExecutionException, InterruptedException {
        when(environmentVariables.getRetainDistributionDays()).thenReturn(0);
        when(environmentVariables.getRetainDistributionVersions()).thenReturn(0);

        CompletableFuture<Void> cleanup = lifeCycleService.cleanup(DISTRIBUTION_NAME,
                                                                   ENVIRONMENT,
                                                                   DEPLOYMENT_ORIGIN_BUCKET);
        cleanup.get();
        verify(s3AsyncClient, never()).listObjects(any(ListObjectsRequest.class));
        verify(s3AsyncClient, never()).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3AsyncClient, never()).deleteObject(any(DeleteObjectRequest.class));
        verify(dynamoDbAsyncClient, never()).deleteItem(any(DeleteItemRequest.class));

    }
}
