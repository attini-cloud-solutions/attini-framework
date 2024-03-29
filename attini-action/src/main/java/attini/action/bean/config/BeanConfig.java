package attini.action.bean.config;

import java.net.URI;
import java.time.Duration;

import attini.action.actions.getdeployorigindata.GetAppDeployOriginDataHandler;
import attini.action.actions.getdeployorigindata.dependencies.DependencyFacade;
import attini.action.actions.posthook.PostPipelineHook;
import attini.action.actions.sam.SamPackageRunnerAdapter;
import attini.action.facades.stackdata.AppDeploymentPlanDataDynamoFacade;
import attini.action.facades.stackdata.AppDeploymentPlanDataFacade;
import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.CloudFormationClientFactory;
import attini.action.S3ClientFactory;
import attini.action.SendUsageDataFacade;
import attini.action.actions.cdk.CdkRunnerAdapter;
import attini.action.actions.deploycloudformation.CfnErrorHandler;
import attini.action.actions.deploycloudformation.CfnHandler;
import attini.action.actions.deploycloudformation.CfnStackFacade;
import attini.action.actions.deploycloudformation.DeployCfnCrossRegionService;
import attini.action.actions.deploycloudformation.DeployCfnService;
import attini.action.actions.deploycloudformation.stackconfig.StackConfigurationFileService;
import attini.action.actions.deploycloudformation.stackconfig.StackConfigurationService;
import attini.action.actions.getdeployorigindata.GetDeployOriginDataHandler;
import attini.action.actions.readoutput.ImportHandler;
import attini.action.actions.runner.Ec2Facade;
import attini.action.actions.runner.EcsFacade;
import attini.action.actions.runner.RunnerHandler;
import attini.action.configuration.InitDeployConfigurationHandler;
import attini.action.custom.resource.CustomResourceResponseSender;
import attini.action.facades.artifactstore.ArtifactStoreFacade;
import attini.action.facades.S3Facade;
import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataDynamoFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stackdata.DistributionDataFacade;
import attini.action.facades.stackdata.InitStackDataDynamoFacade;
import attini.action.facades.stackdata.InitStackDataFacade;
import attini.action.facades.stackdata.ResourceStateDynamoFacade;
import attini.action.facades.stackdata.ResourceStateFacade;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.facades.stepguard.StepGuardFacade;
import attini.action.licence.LicenceAgreementHandler;
import attini.action.system.EnvironmentVariables;
import attini.domain.CustomAwsClient;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.StsClient;

public class BeanConfig {

    @ApplicationScoped
    public StepFunctionFacade stepFunctionFacade(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        SfnClient sfnClient = SfnClient.builder()
                                       .region(Region.of(region))
                                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                       .overrideConfiguration(createClientOverrideConfiguration())
                                       .httpClient(UrlConnectionHttpClient.builder().build())
                                       .endpointOverride(getAwsServiceEndpoint("states", region))
                                       .build();
        return new StepFunctionFacade(sfnClient);
    }

    @ApplicationScoped
    SamPackageRunnerAdapter samPackageRunnerAdapter(RunnerHandler runnerHandler,
                                                    ResourceStateFacade resourceStateFacade,
                                                    StackConfigurationService stackConfigurationService,
                                                    StepFunctionFacade stepFunctionFacade) {
        return new SamPackageRunnerAdapter(runnerHandler,
                                           resourceStateFacade,
                                           stackConfigurationService, stepFunctionFacade);

    }

    @ApplicationScoped
    public EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }

    @ApplicationScoped
    public CloudFormationClientFactory cloudFormationClientFactory(EnvironmentVariables environmentVariables,
                                                                   @CustomAwsClient StsClient stsClient,
                                                                   @CustomAwsClient CloudFormationClient cloudFormationClient) {
        return new CloudFormationClientFactory(environmentVariables, stsClient, cloudFormationClient);
    }

    @ApplicationScoped
    public CdkRunnerAdapter cdkRunnerAdapter(RunnerHandler runnerHandler) {
        return new CdkRunnerAdapter(runnerHandler);
    }

    @ApplicationScoped
    public EcsFacade ecsFacade(EnvironmentVariables environmentVariables, ResourceStateFacade resourceStateFacade) {
        return new EcsFacade(EcsClient.create(), environmentVariables, resourceStateFacade);
    }

    @ApplicationScoped
    ArtifactStoreFacade artifactStoreFacade(EnvironmentVariables environmentVariables,
                                            S3Facade s3Facade,
                                            ObjectMapper objectMapper,
                                            DistributionDataFacade distributionDataFacade) {
        return new ArtifactStoreFacade(s3Facade, String.format("attini-artifact-store-%s-%s",
                                                               environmentVariables.getRegion(),
                                                               environmentVariables.getAccountId()),
                                       objectMapper,
                                       environmentVariables,
                                       distributionDataFacade);

    }

    @ApplicationScoped
    Ec2Facade ec2Facade(EnvironmentVariables environmentVariables, @CustomAwsClient SsmClient ssmClient,
                        ObjectMapper objectMapper, EcsFacade ecsFacade, ResourceStateFacade resourceStateFacade) {
        return new Ec2Facade(Ec2Client.builder()
                                      .httpClient(UrlConnectionHttpClient.builder().build())
                                      .build(), environmentVariables,
                             ssmClient,
                             objectMapper,
                             ecsFacade,
                             resourceStateFacade);
    }

    @ApplicationScoped
    public RunnerHandler runnerHandler(SqsClient sqsClient,
                                       ResourceStateDynamoFacade resourceStateDynamoFacade,
                                       StepFunctionFacade stepFunctionFacade,
                                       EcsFacade ecsFacade, ObjectMapper objectMapper,
                                       Ec2Facade ec2Facade) {
        return new RunnerHandler(sqsClient,
                                 ecsFacade,
                                 resourceStateDynamoFacade,
                                 stepFunctionFacade, objectMapper, ec2Facade);
    }

    @ApplicationScoped
    public S3Facade s3Facade(@CustomAwsClient S3Client s3Client) {
        return new S3Facade(s3Client);
    }


    @ApplicationScoped
    public StackConfigurationFileService stackConfigurationFacade(S3Facade s3Facade,
                                                                  EnvironmentVariables environmentVariables,
                                                                  @CustomAwsClient StsClient stsClient) {
        return new StackConfigurationFileService(stsClient, s3Facade, environmentVariables);
    }

    @ApplicationScoped
    public InitStackDataFacade initStackDataFacade(EnvironmentVariables environmentVariables,
                                                   @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new InitStackDataDynamoFacade(dynamoDbClient, environmentVariables);
    }

    @ApplicationScoped
    @CustomAwsClient
    public SsmClient ssmClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return SsmClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(getAwsServiceEndpoint("ssm", region))
                        .build();
    }

    @ApplicationScoped
    public StackConfigurationService stackConfigurationService(StackConfigurationFileService stackConfigurationFileService,
                                                               CloudFormationClientFactory cloudFormationClientFactory,
                                                               EnvironmentVariables environmentVariables,
                                                               InitStackDataFacade initStackDataFacade,
                                                               @CustomAwsClient SsmClient ssmClient) {
        return new StackConfigurationService(
                stackConfigurationFileService,
                cloudFormationClientFactory,
                environmentVariables,
                ssmClient,
                initStackDataFacade);
    }

    @ApplicationScoped
    public ResourceStateDynamoFacade stackDataDynamoFacade(EnvironmentVariables environmentVariables,
                                                           @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new ResourceStateDynamoFacade(dynamoDbClient,
                                             environmentVariables);
    }

    @ApplicationScoped
    public StepGuardFacade stepGuardFacade(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        LambdaClient lambdaClient = LambdaClient.builder()
                                                .region(Region.of(region))
                                                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                                .overrideConfiguration(createClientOverrideConfiguration())
                                                .httpClient(UrlConnectionHttpClient.builder().build())
                                                .endpointOverride(getAwsServiceEndpoint("lambda", region))
                                                .build();
        return new StepGuardFacade(lambdaClient);
    }

    @ApplicationScoped
    public CfnStackFacade cfnStackFacade(CloudFormationClientFactory cloudFormationClientFactory,
                                         EnvironmentVariables environmentVariables) {
        return new CfnStackFacade(cloudFormationClientFactory,
                                  environmentVariables);
    }

    @ApplicationScoped
    public DeployOriginFacade deployOriginFacade(EnvironmentVariables environmentVariables,
                                                 @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DeployOriginFacade(dynamoDbClient,
                                      environmentVariables);

    }

    @ApplicationScoped
    public SendUsageDataFacade sendUsageDataFacade(EnvironmentVariables environmentVariables,
                                                   StepFunctionFacade stepFunctionFacade,
                                                   DeploymentPlanDataFacade deploymentPlanDataFacade,
                                                   DeployOriginFacade deployOriginFacade,
                                                   ObjectMapper objectMapper) {
        String region = environmentVariables.getRegion();
        SnsAsyncClient snsClient = SnsAsyncClient.builder()
                                                 .region(Region.of(region))
                                                 .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                                 .overrideConfiguration(createClientOverrideConfiguration())
                                                 .httpClient(NettyNioAsyncHttpClient.builder()
                                                                                    .maxConcurrency(50)
                                                                                    .connectionTimeout(Duration.ofSeconds(
                                                                                            1000))
                                                                                    .connectionAcquisitionTimeout(
                                                                                            Duration.ofSeconds(1000))
                                                                                    .maxPendingConnectionAcquires(10_000)
                                                                                    .build())
                                                 .build();
        StsAsyncClient stsClient = StsAsyncClient.builder()
                                                 .region(Region.of(region))
                                                 .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                                 .overrideConfiguration(createClientOverrideConfiguration())
                                                 .endpointOverride(getAwsServiceEndpoint("sts", region))
                                                 .httpClient(NettyNioAsyncHttpClient.builder()
                                                                                    .maxConcurrency(50)
                                                                                    .connectionTimeout(Duration.ofSeconds(
                                                                                            1000))
                                                                                    .connectionAcquisitionTimeout(
                                                                                            Duration.ofSeconds(1000))
                                                                                    .maxPendingConnectionAcquires(10_000)
                                                                                    .build())
                                                 .build();

        OrganizationsClient organizationsClient = OrganizationsClient.builder()
                                                                     .region(Region.AWS_GLOBAL)
                                                                     .credentialsProvider(
                                                                             EnvironmentVariableCredentialsProvider.create())
                                                                     .overrideConfiguration(
                                                                             createClientOverrideConfiguration())
                                                                     .httpClient(UrlConnectionHttpClient.builder()
                                                                                                        .build())
                                                                     .build();
        return new SendUsageDataFacade(stsClient,
                                       organizationsClient,
                                       environmentVariables,
                                       stepFunctionFacade,
                                       snsClient,
                                       deploymentPlanDataFacade,
                                       deployOriginFacade,
                                       objectMapper);
    }


    @ApplicationScoped
    CfnHandler cfnHandler(DeployCfnService deployCfnService,
                          DeployCfnCrossRegionService deployCfnCrossRegionService,
                          EnvironmentVariables environmentVariables,
                          CfnStackFacade cfnStackFacade,
                          StackConfigurationService stackConfigurationService) {
        return new CfnHandler(deployCfnService,
                              deployCfnCrossRegionService,
                              environmentVariables,
                              cfnStackFacade,
                              stackConfigurationService);
    }

    @ApplicationScoped
    DeploymentPlanDataFacade deploymentPlanDataFacade(EnvironmentVariables environmentVariables,
                                                      @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DeploymentPlanDataDynamoFacade(dynamoDbClient, environmentVariables);

    }

    @ApplicationScoped
    CfnErrorHandler cfnErrorHandler(CfnStackFacade cfnStackFacade,
                                    StepGuardFacade stepGuardFacade,
                                    ResourceStateFacade resourceStateFacade,
                                    StepFunctionFacade stepFunctionFacade) {
        return new CfnErrorHandler(cfnStackFacade,
                                   stepGuardFacade,
                                   resourceStateFacade,
                                   stepFunctionFacade);
    }

    @ApplicationScoped
    DeployCfnCrossRegionService deployCfnCrossRegionService(CfnStackFacade cfnStackFacade,
                                                            ResourceStateFacade resourceStateFacade,
                                                            StepFunctionFacade stepFunctionFacade,
                                                            StepGuardFacade stepGuardFacade,
                                                            CfnErrorHandler cfnErrorHandler) {
        return new DeployCfnCrossRegionService(cfnStackFacade,
                                               stepGuardFacade,
                                               resourceStateFacade,
                                               stepFunctionFacade,
                                               cfnErrorHandler);
    }


    @ApplicationScoped
    DeployCfnService deployCfnStackService(CfnStackFacade cfnStackFacade,
                                           ResourceStateFacade resourceStateFacade,
                                           StepFunctionFacade stepFunctionFacade,
                                           CfnErrorHandler cfnErrorHandler) {
        return new DeployCfnService(cfnStackFacade,
                                    resourceStateFacade,
                                    stepFunctionFacade,
                                    cfnErrorHandler);
    }

    @ApplicationScoped
    public DistributionDataFacade distributionDataFacade(EnvironmentVariables environmentVariables,
                                                         @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DistributionDataFacade(environmentVariables, dynamoDbClient);
    }

    @ApplicationScoped
    public GetDeployOriginDataHandler getDeployOriginDataHandler(DeployOriginFacade deployOriginFacade,
                                                                 StepFunctionFacade stepFunctionFacade,
                                                                 SendUsageDataFacade sendUsageDataFacade,
                                                                 ObjectMapper objectMapper,
                                                                 InitStackDataFacade initStackDataFacade,
                                                                 DeploymentPlanDataFacade deploymentPlanDataFacade,
                                                                 @CustomAwsClient CloudFormationClient cloudFormationClient,
                                                                 DependencyFacade dependencyFacade) {
        return new GetDeployOriginDataHandler(deployOriginFacade,
                                              stepFunctionFacade,
                                              cloudFormationClient,
                                              sendUsageDataFacade,
                                              initStackDataFacade,
                                              objectMapper,
                                              deploymentPlanDataFacade,
                                              dependencyFacade);
    }

    @ApplicationScoped
    public AppDeploymentPlanDataFacade appDeploymentPlanDataFacade(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                                                   EnvironmentVariables environmentVariables) {
        return new AppDeploymentPlanDataDynamoFacade(dynamoDbClient, environmentVariables);
    }

    @ApplicationScoped
    public DependencyFacade dependenciesFacade(DistributionDataFacade distributionDataFacade){
        return new DependencyFacade(distributionDataFacade);

    }

    @ApplicationScoped
    public GetAppDeployOriginDataHandler getAppDeployOriginDataHandler(DeployOriginFacade deployOriginFacade,
                                                                       StepFunctionFacade stepFunctionFacade,
                                                                       ObjectMapper objectMapper,
                                                                       DeploymentPlanDataFacade deploymentPlanDataFacade,
                                                                       AppDeploymentPlanDataFacade appDeploymentPlanDataFacade,
                                                                       DependencyFacade dependencyFacade) {
        return new GetAppDeployOriginDataHandler(deployOriginFacade,
                                                 stepFunctionFacade,
                                                 objectMapper,
                                                 deploymentPlanDataFacade,
                                                 appDeploymentPlanDataFacade, dependencyFacade);
    }

    @ApplicationScoped
    public PostPipelineHook postPipelineHook(ObjectMapper objectMapper,
                                             SendUsageDataFacade sendUsageDataFacade,
                                             DeployOriginFacade deployOriginFacade,
                                             DeploymentPlanDataFacade deploymentPlanDataFacade,
                                             ArtifactStoreFacade artifactStoreFacade) {
        return new PostPipelineHook(objectMapper,
                                    sendUsageDataFacade,
                                    deployOriginFacade,
                                    deploymentPlanDataFacade,
                                    artifactStoreFacade);
    }

    @ApplicationScoped
    public S3ClientFactory s3ClientFactory(@CustomAwsClient S3Client s3Client,
                                           @CustomAwsClient StsClient stsClient) {
        return new S3ClientFactory(s3Client, stsClient);
    }

    @ApplicationScoped
    public ImportHandler importHandler(EnvironmentVariables environmentVariables, S3ClientFactory s3ClientFactory) {
        return new ImportHandler(s3ClientFactory, environmentVariables);
    }

    @ApplicationScoped
    public CustomResourceResponseSender customResourceResponseSender() {
        return new CustomResourceResponseSender();
    }

    @ApplicationScoped
    public LicenceAgreementHandler licenceAgreementHandler(SendUsageDataFacade sendUsageDataFacade,
                                                           CustomResourceResponseSender customResourceResponseSender) {
        return new LicenceAgreementHandler(customResourceResponseSender, sendUsageDataFacade);
    }

    @ApplicationScoped
    public InitDeployConfigurationHandler initDeployConfigurationHandler(CustomResourceResponseSender customResourceResponseSender,
                                                                         ObjectMapper objectMapper) {
        return new InitDeployConfigurationHandler(customResourceResponseSender, objectMapper);
    }

    @ApplicationScoped
    @CustomAwsClient
    public StsClient stsClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return StsClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(getAwsServiceEndpoint("sts", region))
                        .build();
    }

    @ApplicationScoped
    @CustomAwsClient
    public S3Client s3Client(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return S3Client.builder()
                       .region(Region.of(region))
                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                       .overrideConfiguration(createClientOverrideConfiguration())
                       .httpClient(UrlConnectionHttpClient.builder().build())
                       .endpointOverride(getAwsServiceEndpoint("s3", region))
                       .build();
    }

    @ApplicationScoped
    @CustomAwsClient
    public CloudFormationClient cloudFormationClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return CloudFormationClient.builder()
                                   .region(Region.of(region))
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(getAwsServiceEndpoint("cloudformation", region))
                                   .build();
    }

    @ApplicationScoped
    @CustomAwsClient
    public DynamoDbClient dynamoDbClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return DynamoDbClient.builder()
                             .region(Region.of(region))
                             .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                             .overrideConfiguration(createClientOverrideConfiguration())
                             .httpClient(UrlConnectionHttpClient.builder().build())
                             .endpointOverride(getAwsServiceEndpoint("dynamodb", region))
                             .build();
    }

    public static ClientOverrideConfiguration createClientOverrideConfiguration() {
        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofSeconds(240))
                                          .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .numRetries(20)
                                                                  .build())
                                          .build();
    }

    public static URI getAwsServiceEndpoint(String service, String region) {
        return URI.create(String.format("https://%s.%s.amazonaws.com", service, region));
    }

}
