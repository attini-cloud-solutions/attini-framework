/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static java.util.Objects.requireNonNull;

import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

public class CloudFormationClientFactory {

    private final EnvironmentVariables environmentVariables;
    private final ClientOverrideConfiguration clientOverrideConfiguration;
    private final StsClient stsClient;
    private final CloudFormationClient cloudFormationClient;

    public CloudFormationClientFactory(EnvironmentVariables environmentVariables,
                                       ClientOverrideConfiguration clientOverrideConfiguration,
                                       StsClient stsClient, CloudFormationClient cloudFormationClient) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.clientOverrideConfiguration = requireNonNull(clientOverrideConfiguration, "clientOverrideConfiguration");
        this.stsClient = requireNonNull(stsClient, "stsClient");
        this.cloudFormationClient = requireNonNull(cloudFormationClient, "cloudFormationClient");
    }

    public CloudFormationClient getClient(CloudFormationEvent cloudFormationEvent) {
        if (cloudFormationEvent.getExecutionRoleArn().isPresent() && cloudFormationEvent.getRegion().isPresent()) {
            return getCloudFormationClient(Region.of(cloudFormationEvent.getRegion().get()),
                                           cloudFormationEvent.getExecutionRoleArn().get());

        }
        if (cloudFormationEvent.getExecutionRoleArn().isPresent()) {
            return getCloudFormationClient(cloudFormationEvent.getExecutionRoleArn().get());
        }

        if (cloudFormationEvent.getRegion().isPresent()) {
            return getCloudFormationClient(Region.of(cloudFormationEvent.getRegion().get()));
        }

        return getCloudFormationClient();
    }

    public CloudFormationClient getCloudFormationClient() {
        return cloudFormationClient;
    }


    public CloudFormationClient getCloudFormationClient(Region region) {
        return CloudFormationClient.builder()
                                   .region(region)
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .overrideConfiguration(clientOverrideConfiguration)
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(
                                           URI.create(String.format("https://%s.%s.amazonaws.com",
                                                                    "cloudformation",
                                                                    region.toString())))
                                   .build();
    }

    public CloudFormationClient getCloudFormationClient(String executionRole) {
        return getCloudFormationClient(null, executionRole);
    }

    public CloudFormationClient getCloudFormationClient(Region region, String executionRole) {

        String role = "arn:aws:iam::" + environmentVariables.getAccountId() + ":role/attini/attini-action-role-" + environmentVariables
                .getRegion();

        Credentials cfnCredentials = stsClient
                .assumeRole(AssumeRoleRequest.builder()
                                             .roleArn(role)
                                             .roleSessionName("GetAttiniCfnDeployRole")
                                             .build())
                .credentials();
        StsClientBuilder stsClientBuilder = StsClient.builder()
                                                     .region(Region.of(environmentVariables.getRegion()))
                                                     .credentialsProvider(StaticCredentialsProvider.create(
                                                             AwsSessionCredentials.create(
                                                                     cfnCredentials.accessKeyId(),
                                                                     cfnCredentials.secretAccessKey(),
                                                                     cfnCredentials.sessionToken())))
                                                     .overrideConfiguration(clientOverrideConfiguration)
                                                     .httpClient(UrlConnectionHttpClient.builder().build())
                                                     .endpointOverride(
                                                             URI.create(String.format("https://%s.%s.amazonaws.com",
                                                                                      "sts",
                                                                                      environmentVariables.getRegion())
                                                             ));

        try (StsClient newStsClient = stsClientBuilder.build()) {


            Credentials credentials = newStsClient
                    .assumeRole(AssumeRoleRequest.builder()
                                                 .roleArn(executionRole)
                                                 .roleSessionName("AttiniCfnDeploy")
                                                 .build())
                    .credentials();

            String destinationRegion = region != null ? region.toString() : environmentVariables.getRegion();

            return CloudFormationClient.builder()
                                       .region(Region.of(destinationRegion))
                                       .credentialsProvider(StaticCredentialsProvider.create(
                                               AwsSessionCredentials.create(
                                                       credentials.accessKeyId(),
                                                       credentials.secretAccessKey(),
                                                       credentials.sessionToken())))
                                       .overrideConfiguration(clientOverrideConfiguration)
                                       .httpClient(UrlConnectionHttpClient.builder().build())
                                       .endpointOverride(URI.create(String.format("https://%s.%s.amazonaws.com",
                                                                                  "cloudformation",
                                                                                  destinationRegion)))
                                       .build();
        }
    }
}
