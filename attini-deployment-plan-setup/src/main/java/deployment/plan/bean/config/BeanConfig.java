package deployment.plan.bean.config;

import java.net.URI;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.CustomAwsClient;
import deployment.plan.custom.resource.CfnResponseSender;
import deployment.plan.custom.resource.CustomResourceHandler;
import deployment.plan.custom.resource.service.AppDeploymentService;
import deployment.plan.custom.resource.service.DeployStatesFacade;
import deployment.plan.custom.resource.service.DeploymentPlanStateFactory;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataService;
import deployment.plan.custom.resource.service.RegisterDeploymentPlanTriggerService;
import deployment.plan.system.EnvironmentVariables;
import deployment.plan.transform.AttiniStepLoader;
import deployment.plan.transform.DeployData;
import deployment.plan.transform.DeploymentPlanStepsCreator;
import deployment.plan.transform.TemplateFileLoader;
import deployment.plan.transform.TemplateFileLoaderImpl;
import jakarta.enterprise.context.ApplicationScoped;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class BeanConfig {

    @ApplicationScoped
    public EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }


    @ApplicationScoped
    public TemplateFileLoader templateFileLoader() {
        return new TemplateFileLoaderImpl();
    }

    @ApplicationScoped
    public AttiniStepLoader attiniStepLoader(TemplateFileLoader templateFileLoader,
                                             ObjectMapper objectMapper,
                                             EnvironmentVariables environmentVariables) {
        return new AttiniStepLoader(templateFileLoader, objectMapper, environmentVariables);
    }

    @ApplicationScoped
    public DeployData deployData(TemplateFileLoader templateFileLoader) {
        return new DeployData(templateFileLoader);
    }

    @ApplicationScoped
    public DeploymentPlanStepsCreator deploymentPlanStepsCreator(AttiniStepLoader attiniStepLoader,
                                                                 DeployData deployData,
                                                                 ObjectMapper objectMapper) {
        return new DeploymentPlanStepsCreator(attiniStepLoader, deployData, objectMapper);
    }

    @ApplicationScoped
    public Ec2Client ec2Client(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return Ec2Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(getAwsServiceEndpoint("ec2", region))
                        .build();
    }

    @ApplicationScoped
    DeployStatesFacade deployStatesFacade(EnvironmentVariables environmentVariables,
                                          ObjectMapper objectMapper,
                                          @CustomAwsClient DynamoDbClient dynamoDbClient) {
        return new DeployStatesFacade(dynamoDbClient, environmentVariables, objectMapper);
    }

    @ApplicationScoped
    RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService(DeployStatesFacade deployStatesFacade) {
        return new RegisterDeploymentPlanTriggerService(deployStatesFacade);
    }


    @ApplicationScoped
    RegisterDeployOriginDataService registerDeployOriginDataService(DeployStatesFacade deployStatesFacade,
                                                                   DeploymentPlanStateFactory deploymentPlanStateFactory) {
        return new RegisterDeployOriginDataService(deployStatesFacade, deploymentPlanStateFactory);
    }


    @ApplicationScoped
    @CustomAwsClient
    public DynamoDbClient dynamoDbClient(EnvironmentVariables environmentVariables) {
        return DynamoDbClient.builder()
                             .region(Region.of(environmentVariables.getRegion()))
                             .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                             .overrideConfiguration(createClientOverrideConfiguration())
                             .httpClient(UrlConnectionHttpClient.builder().build())
                             .endpointOverride(getAwsServiceEndpoint("dynamodb", environmentVariables.getRegion()))
                             .build();
    }

    @ApplicationScoped
    @CustomAwsClient
    public CloudFormationClient cloudFormationClient(EnvironmentVariables environmentVariables) {
        return CloudFormationClient.builder()
                                   .region(Region.of(environmentVariables.getRegion()))
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(getAwsServiceEndpoint("cloudFormation",
                                                                           environmentVariables.getRegion()))
                                   .build();
    }

    @ApplicationScoped
    DeploymentPlanStateFactory deploymentPlanStateFactory(@CustomAwsClient CloudFormationClient cloudFormationClient, DeployStatesFacade deployStatesFacade){
        return new DeploymentPlanStateFactory(cloudFormationClient, deployStatesFacade);
    }

    @ApplicationScoped
    AppDeploymentService appDeploymentService(DeployStatesFacade deployStatesFacade, DeploymentPlanStateFactory deploymentPlanStateFactory){
        return new AppDeploymentService(deployStatesFacade, deploymentPlanStateFactory);
    }

    @ApplicationScoped
    public CustomResourceHandler customResourceHandler(RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService,
                                                       RegisterDeployOriginDataService registerDeployOriginDataService,
                                                       ObjectMapper objectMapper, AppDeploymentService appDeploymentService) {


        return new CustomResourceHandler(registerDeploymentPlanTriggerService,
                                         registerDeployOriginDataService,
                                         new CfnResponseSender(),
                                         objectMapper, appDeploymentService);
    }

    private static ClientOverrideConfiguration createClientOverrideConfiguration() {

        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofSeconds(240))
                                          .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .numRetries(20)
                                                                  .build())
                                          .build();
    }

    private static URI getAwsServiceEndpoint(String service, String environment) {
        return URI.create(String.format("https://%s.%s.amazonaws.com", service, environment));
    }

}
