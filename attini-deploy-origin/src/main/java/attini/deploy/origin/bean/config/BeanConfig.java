package attini.deploy.origin.bean.config;


import java.net.URI;
import java.time.Duration;

import attini.deploy.origin.appdeployment.AppDeploymentDataFacade;
import attini.deploy.origin.appdeployment.AppDeploymentFacade;
import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import attini.deploy.origin.DistributionDataFacade;
import attini.deploy.origin.InitDeployEventFactory;
import attini.deploy.origin.InitDeployService;
import attini.deploy.origin.MonitoringFacade;
import attini.deploy.origin.PublishArtifactService;
import attini.deploy.origin.PutLatestDistributionReferenceParameter;
import attini.deploy.origin.SystemClockFacade;
import attini.deploy.origin.config.AttiniConfigFactory;
import attini.deploy.origin.config.ConfigFileResolver;
import attini.deploy.origin.config.InitDeployParameterService;
import attini.deploy.origin.deploystack.DeployDataFacade;
import attini.deploy.origin.deploystack.DeployInitStackService;
import attini.deploy.origin.deploystack.DynamoDeployDataFacade;
import attini.deploy.origin.deploystack.DynamoInitDeployStackFacade;
import attini.deploy.origin.lifecycle.LifeCycleService;
import attini.deploy.origin.s3.S3Facade;
import attini.deploy.origin.s3.TagOriginObjectService;
import attini.deploy.origin.stepguard.StepGuardFacade;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.CustomAwsClient;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

@ApplicationScoped
public class BeanConfig {


    @ApplicationScoped
    InitDeployParameterService initDeployParameterService(DistributionDataFacade distributionDataFacade,
                                                          S3Facade s3Facade,
                                                          EnvironmentVariables environmentVariables) {
        return new InitDeployParameterService(distributionDataFacade, s3Facade, environmentVariables);
    }

    @ApplicationScoped
    public DynamoDeployDataFacade dynamoDeployDataFacade(EnvironmentVariables environmentVariables,
                                                         @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DynamoDeployDataFacade(dynamoDbClient, environmentVariables);
    }

    @ApplicationScoped
    InitDeployEventFactory initDeployEventFactory(ObjectMapper objectMapper) {
        return new InitDeployEventFactory(objectMapper);
    }


    @ApplicationScoped
    public EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }

    @ApplicationScoped
    public AttiniConfigFactory attiniConfigFactory(EnvironmentVariables environmentVariables,
                                                   InitDeployParameterService initDeployParameterService) {
        return new AttiniConfigFactory(new ConfigFileResolver(
                environmentVariables), initDeployParameterService);
    }

    @ApplicationScoped
    public DeployInitStackService deployInitStackService(EnvironmentVariables environmentVariables,
                                                         @CustomAwsClient CloudFormationClient cloudFormationClient) {
        return new DeployInitStackService(cloudFormationClient,
                                          environmentVariables);

    }

    @ApplicationScoped
    public StepGuardFacade stepGuardFacade(EnvironmentVariables environmentVariables,
                                           ObjectMapper objectMapper,
                                           @CustomAwsClient LambdaClient lambdaClient) {
        return new StepGuardFacade(lambdaClient, environmentVariables, objectMapper);
    }

    @ApplicationScoped
    public TagOriginObjectService tagOriginObjectService(@CustomAwsClient S3Client s3Client) {
        return new TagOriginObjectService(s3Client);
    }

    @ApplicationScoped
    public S3Facade s3Facade(@CustomAwsClient S3Client s3Client, @CustomAwsClient S3AsyncClient s3AsyncClient) {
        return new S3Facade(s3Client, s3AsyncClient);
    }

    @ApplicationScoped
    public PublishArtifactService publishArtifactService(S3Facade s3Facade,
                                                         AttiniConfigFactory attiniConfigFactory,
                                                         EnvironmentVariables environmentVariables,
                                                         TagOriginObjectService tagOriginObjectService) {
        return new PublishArtifactService(s3Facade,
                                          attiniConfigFactory,
                                          environmentVariables,
                                          tagOriginObjectService);
    }

    @ApplicationScoped
    public MonitoringFacade monitoringFacade(EnvironmentVariables environmentVariables,
                                             ObjectMapper objectMapper) {
        SnsClient snsClient = SnsClient.builder()
                                       .region(Region.of(environmentVariables.getAwsRegion()))
                                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                       .overrideConfiguration(defaultClientOverride())
                                       .httpClient(UrlConnectionHttpClient.builder().build())
                                       .build();
        return new MonitoringFacade(snsClient, environmentVariables, objectMapper);
    }

    @ApplicationScoped
    public DynamoInitDeployStackFacade dynamoInitDeployStackFacade(EnvironmentVariables environmentVariables,
                                                                   @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DynamoInitDeployStackFacade(dynamoDbClient, environmentVariables);
    }

    @ApplicationScoped
    public LifeCycleService lifeCycleService(EnvironmentVariables environmentVariables,
                                             DeployDataFacade deployDataFacade,
                                             @CustomAwsClient S3AsyncClient s3AsyncClient,
                                             @CustomAwsClient DynamoDbAsyncClient dynamoDbAsyncClient) {
        return new LifeCycleService(environmentVariables,
                                    deployDataFacade,
                                    dynamoDbAsyncClient,
                                    s3AsyncClient);
    }

    @ApplicationScoped
    public DistributionDataFacade distributionDataFacade(EnvironmentVariables environmentVariables,
                                                         @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DistributionDataFacade(dynamoDbClient, environmentVariables);
    }

    @ApplicationScoped
    public AppDeploymentDataFacade appDeploymentDataFacade(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                                           EnvironmentVariables environmentVariables) {
        return new AppDeploymentDataFacade(environmentVariables, dynamoDbClient);
    }

    @ApplicationScoped
    public AppDeploymentFacade appDeploymentFacade(@CustomAwsClient SfnClient sfnClient,
                                                   AppDeploymentDataFacade appDeploymentDataFacade,
                                                   ObjectMapper objectMapper, DeployDataFacade deployDataFacade) {
        return new AppDeploymentFacade(sfnClient, appDeploymentDataFacade, objectMapper, deployDataFacade);
    }

    @ApplicationScoped
    public InitDeployService initDeployService(PublishArtifactService publishArtifactService,
                                               DeployInitStackService deployInitStackService,
                                               DeployDataFacade deployDataFacade,
                                               DynamoInitDeployStackFacade dynamoInitDeployStackFacade,
                                               LifeCycleService lifeCycleService,
                                               MonitoringFacade monitoringFacade,
                                               StepGuardFacade stepGuardFacade,
                                               DistributionDataFacade distributionDataFacade,
                                               @CustomAwsClient SsmClient ssmClient, AppDeploymentFacade appDeploymentFacade) {
        return new InitDeployService(publishArtifactService,
                                     deployInitStackService,
                                     deployDataFacade,
                                     new PutLatestDistributionReferenceParameter(ssmClient),
                                     dynamoInitDeployStackFacade,
                                     lifeCycleService, monitoringFacade,
                                     new SystemClockFacade(),
                                     stepGuardFacade,
                                     distributionDataFacade,
                                     appDeploymentFacade);
    }

    @CustomAwsClient
    @ApplicationScoped
    public SsmClient ssmClient(EnvironmentVariables environmentVariables) {
        return SsmClient.builder()
                        .region(Region.of(environmentVariables.getAwsRegion()))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(defaultClientOverride())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(
                                getAwsServiceEndpoint("ssm", environmentVariables.getAwsRegion())
                        )
                        .build();
    }


    @CustomAwsClient
    @ApplicationScoped
    public LambdaClient lambdaClient(EnvironmentVariables environmentVariables) {
        return LambdaClient.builder()
                           .region(Region.of(environmentVariables.getAwsRegion()))
                           .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                           .overrideConfiguration(defaultClientOverride())
                           .httpClient(UrlConnectionHttpClient.builder()
                                                              .connectionTimeout(Duration.ofSeconds(20))
                                                              .build())
                           .endpointOverride(
                                   getAwsServiceEndpoint("lambda", environmentVariables.getAwsRegion())
                           )
                           .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public S3Client s3Client(EnvironmentVariables environmentVariables) {
        return S3Client.builder()
                       .region(Region.of(environmentVariables.getAwsRegion()))
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .overrideConfiguration(defaultClientOverride())
                       .httpClient(UrlConnectionHttpClient.builder().build())
                       .endpointOverride(getAwsServiceEndpoint("s3", environmentVariables.getAwsRegion())
                       )
                       .build();
    }


    @CustomAwsClient
    @ApplicationScoped
    public S3AsyncClient s3AsyncClient(EnvironmentVariables environmentVariables) {
        return S3AsyncClient.builder()
                            .region(Region.of(environmentVariables.getAwsRegion()))
                            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                            .httpClient(NettyNioAsyncHttpClient.builder()
                                                               .maxConcurrency(800)
                                                               .connectionMaxIdleTime(Duration.ofSeconds(800))
                                                               .connectionTimeout(Duration.ofSeconds(800))
                                                               .connectionAcquisitionTimeout(Duration.ofSeconds(20))
                                                               .maxPendingConnectionAcquires(10_000)
                                                               .build())
                            .overrideConfiguration(defaultClientOverride())
                            .endpointOverride(
                                    getAwsServiceEndpoint("s3", environmentVariables.getAwsRegion())
                            )
                            .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient(EnvironmentVariables environmentVariables) {
        return DynamoDbClient.builder()
                             .region(Region.of(environmentVariables.getAwsRegion()))
                             .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                             .overrideConfiguration(defaultClientOverride())
                             .httpClient(UrlConnectionHttpClient.builder().build())
                             .endpointOverride(
                                     getAwsServiceEndpoint("dynamodb", environmentVariables.getAwsRegion())
                             )
                             .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public SfnClient sfnClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getAwsRegion();
        return SfnClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(defaultClientOverride())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(getAwsServiceEndpoint("states", region))
                        .build();

    }

    @CustomAwsClient
    @ApplicationScoped
    public DynamoDbAsyncClient dynamoDbAsyncClient(EnvironmentVariables environmentVariables) {
        return DynamoDbAsyncClient.builder()
                                  .region(Region.of(environmentVariables.getAwsRegion()))
                                  .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                  .overrideConfiguration(defaultClientOverride())
                                  .httpClient(NettyNioAsyncHttpClient.builder()
                                                                     .maxConcurrency(1000)
                                                                     .connectionTimeout(Duration.ofSeconds(1000))
                                                                     .connectionAcquisitionTimeout(Duration.ofSeconds(
                                                                             1000))
                                                                     .maxPendingConnectionAcquires(10_000)
                                                                     .build())
                                  .endpointOverride(
                                          getAwsServiceEndpoint("dynamodb", environmentVariables.getAwsRegion())
                                  )
                                  .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public CloudFormationClient cloudFormationClient(EnvironmentVariables environmentVariables) {
        return CloudFormationClient.builder()
                                   .region(Region.of(environmentVariables.getAwsRegion()))
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .overrideConfiguration(defaultClientOverride())
                                   .endpointOverride(getAwsServiceEndpoint("cloudformation",
                                                                           environmentVariables.getAwsRegion())
                                   )
                                   .build();
    }

    private static ClientOverrideConfiguration defaultClientOverride() {
        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofSeconds(240))
                                          .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .numRetries(20)
                                                                  .build())
                                          .build();
    }

    private URI getAwsServiceEndpoint(String service, String region) {
        return URI.create(String.format("https://%s.%s.amazonaws.com", service, region));
    }
}
