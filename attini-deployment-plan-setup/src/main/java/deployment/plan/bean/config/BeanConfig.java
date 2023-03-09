package deployment.plan.bean.config;

import java.net.URI;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.CustomAwsClient;
import deployment.plan.custom.resource.CfnResponseSender;
import deployment.plan.custom.resource.CustomResourceHandler;
import deployment.plan.custom.resource.service.DeployStatesFacade;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataService;
import deployment.plan.custom.resource.service.RegisterDeploymentPlanTriggerService;
import deployment.plan.system.EnvironmentVariables;
import deployment.plan.transform.AttiniStepLoader;
import deployment.plan.transform.DeployData;
import deployment.plan.transform.DeploymentPlanStepsCreator;
import deployment.plan.transform.TemplateFileLoader;
import deployment.plan.transform.TemplateFileLoaderImpl;
import deployment.plan.transform.TransformDeploymentPlanCloudFormation;
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
    public AttiniStepLoader attiniStepLoader(TemplateFileLoader templateFileLoader, ObjectMapper objectMapper) {
        return new AttiniStepLoader(templateFileLoader, objectMapper);
    }

    @ApplicationScoped
    public DeployData deployData(TemplateFileLoader templateFileLoader) {
        return new DeployData(templateFileLoader);
    }

    @ApplicationScoped
    public DeploymentPlanStepsCreator deploymentPlanStepsCreator(AttiniStepLoader attiniStepLoader,
                                                                 DeployData deployData,
                                                                 ObjectMapper objectMapper,
                                                                 EnvironmentVariables environmentVariables) {
        return new DeploymentPlanStepsCreator(attiniStepLoader, deployData, objectMapper, environmentVariables);
    }

    @ApplicationScoped
    public TransformDeploymentPlanCloudFormation transformDeploymentPlanCloudFormation(EnvironmentVariables environmentVariables,
                                                                                       ObjectMapper objectMapper,
                                                                                       DeploymentPlanStepsCreator deploymentPlanStepsCreator) {

        Ec2Client ec2Client = Ec2Client.builder()
                                 .region(Region.of(environmentVariables.getRegion()))
                                 .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                 .overrideConfiguration(createClientOverrideConfiguration())
                                 .httpClient(UrlConnectionHttpClient.builder().build())
                                 .endpointOverride(getAwsServiceEndpoint("ec2", environmentVariables.getRegion()))
                                 .build();
        return new TransformDeploymentPlanCloudFormation(environmentVariables,
                                                         ec2Client,
                                                         objectMapper,
                                                         deploymentPlanStepsCreator);
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
                                                                    @CustomAwsClient CloudFormationClient cloudFormationClient) {
        return new RegisterDeployOriginDataService(deployStatesFacade, cloudFormationClient);
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
    public CustomResourceHandler customResourceHandler(RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService,
                                                       RegisterDeployOriginDataService registerDeployOriginDataService,
                                                       ObjectMapper objectMapper) {


        return new CustomResourceHandler(registerDeploymentPlanTriggerService,
                                         registerDeployOriginDataService,
                                         new CfnResponseSender(),
                                         objectMapper);
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
