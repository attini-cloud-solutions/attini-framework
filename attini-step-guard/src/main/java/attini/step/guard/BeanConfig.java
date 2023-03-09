/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import java.net.URI;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;

import attini.domain.CustomAwsClient;
import attini.step.guard.cdk.RegisterCdkStacksService;
import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.stackdata.StackDataDynamoFacade;
import attini.step.guard.stackdata.StackDataFacade;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sts.StsClient;

@ApplicationScoped
public class BeanConfig {


    @ApplicationScoped
    RegisterCdkStacksService registerCdkStacksService(StackDataFacade stackDataFacade,
                                                      EnvironmentVariables environmentVariables) {
        return new RegisterCdkStacksService(stackDataFacade, environmentVariables);
    }

    @ApplicationScoped
    EventTypeResolver eventTypeResolver() {
        return new EventTypeResolver();
    }

    @ApplicationScoped
    StackErrorResolver stackErrorResolver(CloudFormationClientFactory cloudFormationClientFactory) {
        return new StackErrorResolver(cloudFormationClientFactory);
    }

    @ApplicationScoped
    PublishEventService publishEventService(SnsClient snsClient, EnvironmentVariables environmentVariables) {
        return new PublishEventService(snsClient, environmentVariables);
    }

    @ApplicationScoped
    DeployDataFacade deployDataFacade(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                      EnvironmentVariables environmentVariables) {
        return new DeployDataFacade(environmentVariables, dynamoDbClient);
    }

    @ApplicationScoped
    SfnResponseSender sfnResponseSender(@CustomAwsClient SfnClient sfnClient) {
        return new SfnResponseSender(sfnClient);
    }

    @ApplicationScoped
    RespondToCfnEvent respondToCfnEvent(SfnResponseSender sfnResponseSender,
                                        @CustomAwsClient SfnClient sfnClient,
                                        StackDataFacade stackDataFacade,
                                        CfnOutputCreator cfnOutputCreator,
                                        StackErrorResolver stackErrorResolver,
                                        PublishEventService publishEventService,
                                        DeployDataFacade deployDataFacade) {
        return new RespondToCfnEvent(sfnResponseSender,
                                     sfnClient,
                                     stackDataFacade,
                                     cfnOutputCreator,
                                     stackErrorResolver,
                                     publishEventService,
                                     deployDataFacade);
    }

    @ApplicationScoped
    CloudFormationClientFactory cloudFormationClientFactory(EnvironmentVariables environmentVariables,
                                                            @CustomAwsClient StsClient stsClient,
                                                            @CustomAwsClient CloudFormationClient cloudFormationClient) {
        return new CloudFormationClientFactory(environmentVariables,
                                               createClientOverrideConfiguration(),
                                               stsClient,
                                               cloudFormationClient);
    }

    @ApplicationScoped
    CfnOutputCreator cfnOutputCreator(CloudFormationClientFactory cloudFormationClientFactory) {
        return new CfnOutputCreator(cloudFormationClientFactory);
    }

    @ApplicationScoped
    EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }


    @ApplicationScoped
    ContinueExecutionService continueExecutionService(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                                      SfnResponseSender sfnResponseSender,
                                                      EnvironmentVariables environmentVariables) {
        return new ContinueExecutionService(dynamoDbClient, sfnResponseSender, environmentVariables);
    }

    @ApplicationScoped
    StackDataFacade stackDataFacade(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                    EnvironmentVariables environmentVariables) {
        return new StackDataDynamoFacade(dynamoDbClient, environmentVariables);
    }

    @CustomAwsClient
    @ApplicationScoped
    CloudFormationClient cloudFormationClient() {
        return CloudFormationClient.builder()
                                   .region(Region.of(System.getenv("AWS_REGION")))
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(
                                           getAwsServiceEndpoint("cloudformation")
                                   )
                                   .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public SfnClient sfnClient() {
        return SfnClient.builder()
                        .region(Region.of(System.getenv("AWS_REGION")))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(getAwsServiceEndpoint("states"))
                        .build();

    }

    @CustomAwsClient
    @ApplicationScoped
    public StsClient stsClient() {
        return StsClient.builder()
                        .region(Region.of(System.getenv("AWS_REGION")))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(
                                getAwsServiceEndpoint("sts")
                        )
                        .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {

        return DynamoDbClient.builder()
                             .region(Region.of(System.getenv("AWS_REGION")))
                             .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                             .overrideConfiguration(createClientOverrideConfiguration())
                             .httpClient(UrlConnectionHttpClient.builder().build())
                             .endpointOverride(getAwsServiceEndpoint("dynamodb"))
                             .build();

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


    private static URI getAwsServiceEndpoint(String service) {
        return URI.create(String.format("https://%s.%s.amazonaws.com", service, System.getenv("AWS_REGION")));
    }
}
