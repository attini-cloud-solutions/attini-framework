/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import java.net.URI;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.CustomAwsClient;
import attini.step.guard.cdk.RegisterCdkStacksService;
import attini.step.guard.cloudformation.CfnEventHandler;
import attini.step.guard.cloudformation.CfnOutputCreator;
import attini.step.guard.cloudformation.CfnSnsEventTypeResolver;
import attini.step.guard.cloudformation.CloudFormationClientFactory;
import attini.step.guard.cloudformation.InitDeployEventHandler;
import attini.step.guard.cloudformation.StackErrorResolver;
import attini.step.guard.deploydata.DeployDataFacade;
import attini.step.guard.manualapproval.ContinueExecutionService;
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
import software.amazon.awssdk.services.sts.StsClient;

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
    DeployDataFacade deployDataFacade(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                      EnvironmentVariables environmentVariables) {
        return new DeployDataFacade(environmentVariables, dynamoDbClient);
    }

    @ApplicationScoped
    StepFunctionFacade stepFunctionFacade(@CustomAwsClient SfnClient sfnClient) {
        return new StepFunctionFacade(sfnClient);
    }

    @ApplicationScoped
    CfnSnsEventTypeResolver cfnSnsEventTypeResolver(){
        return new CfnSnsEventTypeResolver();
    }

    @ApplicationScoped
    InitDeployEventHandler initDeployEventHandler(StackDataFacade stackDataFacade,
                                                  DeployDataFacade deployDataFacade,
                                                  StepFunctionFacade stepFunctionFacade,
                                                  StackErrorResolver stackErrorResolver,
                                                  CfnSnsEventTypeResolver cfnSnsEventTypeResolver) {
        return new InitDeployEventHandler(stackDataFacade, deployDataFacade, stepFunctionFacade, stackErrorResolver, cfnSnsEventTypeResolver);
    }

    @ApplicationScoped
    CfnEventHandler respondToCfnEvent(StepFunctionFacade stepFunctionFacade,
                                      StackDataFacade stackDataFacade,
                                      CfnOutputCreator cfnOutputCreator,
                                      StackErrorResolver stackErrorResolver,
                                      DeployDataFacade deployDataFacade,
                                      CfnSnsEventTypeResolver cfnSnsEventTypeResolver) {
        return new CfnEventHandler(stepFunctionFacade,
                                   stackDataFacade,
                                   cfnOutputCreator,
                                   stackErrorResolver,
                                   deployDataFacade,
                                   cfnSnsEventTypeResolver);
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
    CfnOutputCreator cfnOutputCreator(CloudFormationClientFactory cloudFormationClientFactory,
                                      ObjectMapper objectMapper) {
        return new CfnOutputCreator(cloudFormationClientFactory, objectMapper);
    }

    @ApplicationScoped
    EnvironmentVariables environmentVariables() {
        return new EnvironmentVariables();
    }


    @ApplicationScoped
    ContinueExecutionService continueExecutionService(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                                      StepFunctionFacade stepFunctionFacade,
                                                      EnvironmentVariables environmentVariables) {
        return new ContinueExecutionService(dynamoDbClient, stepFunctionFacade, environmentVariables);
    }

    @ApplicationScoped
    StackDataFacade stackDataFacade(@CustomAwsClient DynamoDbClient dynamoDbClient,
                                    EnvironmentVariables environmentVariables) {
        return new StackDataDynamoFacade(dynamoDbClient, environmentVariables);
    }

    @CustomAwsClient
    @ApplicationScoped
    CloudFormationClient cloudFormationClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return CloudFormationClient.builder()
                                   .region(Region.of(region))
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(getAwsServiceEndpoint("cloudformation", region))
                                   .build();
    }

    @CustomAwsClient
    @ApplicationScoped
    public SfnClient sfnClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return SfnClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(getAwsServiceEndpoint("states", region))
                        .build();

    }

    @CustomAwsClient
    @ApplicationScoped
    public StsClient stsClient(EnvironmentVariables environmentVariables) {
        String region = environmentVariables.getRegion();
        return StsClient.builder()
                        .region(Region.of(region))
                        .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .overrideConfiguration(createClientOverrideConfiguration())
                        .httpClient(UrlConnectionHttpClient.builder().build())
                        .endpointOverride(
                                getAwsServiceEndpoint("sts", region)
                        )
                        .build();
    }

    @CustomAwsClient
    @ApplicationScoped
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


    private static ClientOverrideConfiguration createClientOverrideConfiguration() {

        return ClientOverrideConfiguration.builder()
                                          .apiCallTimeout(Duration.ofSeconds(240))
                                          .apiCallAttemptTimeout(Duration.ofSeconds(30))
                                          .retryPolicy(RetryPolicy.builder()
                                                                  .numRetries(20)
                                                                  .build())
                                          .build();

    }


    private static URI getAwsServiceEndpoint(String service, String region) {
        return URI.create(String.format("https://%s.%s.amazonaws.com", service, region));
    }
}
