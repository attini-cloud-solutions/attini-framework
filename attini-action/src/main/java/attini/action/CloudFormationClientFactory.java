/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action;

import static attini.action.bean.config.BeanConfig.createClientOverrideConfiguration;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import attini.action.system.EnvironmentVariables;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

public class CloudFormationClientFactory {

    private final Map<GetCloudFormationClientRequest, CloudFormationClient> cache = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(CloudFormationClientFactory.class);


    private final EnvironmentVariables environmentVariables;
    private final StsClient stsClient;
    private final CloudFormationClient cloudFormationClient;

    public CloudFormationClientFactory(EnvironmentVariables environmentVariables,
                                       StsClient stsClient,
                                       CloudFormationClient cloudFormationClient) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.stsClient = requireNonNull(stsClient, "stsClient");
        this.cloudFormationClient = requireNonNull(cloudFormationClient, "cloudFormationClient");
    }

    public CloudFormationClient getClient(GetCloudFormationClientRequest request) {
        return cache.computeIfAbsent(request, getCloudFormationClientRequest -> createClient(request));
    }

    public CloudFormationClient createClient(GetCloudFormationClientRequest request) {

        if (request.getExecutionRoleArn().isPresent() && request.getRegion().isPresent()) {
            return getCloudFormationClient(Region.of(request.getRegion().get()),
                                           request.getExecutionRoleArn().get());

        }
        if (request.getExecutionRoleArn().isPresent()) {
            return getCloudFormationClient(request.getExecutionRoleArn().get());
        }

        if (request.getRegion().isPresent()) {
            return getCloudFormationClient(Region.of(request.getRegion().get()));
        }

        return cloudFormationClient;
    }


    private CloudFormationClient getCloudFormationClient(Region region) {

        logger.info("Creating cloudformation client with region=" + region);

        return CloudFormationClient.builder()
                                   .region(region)
                                   .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(
                                           URI.create(String.format("https://%s.%s.amazonaws.com",
                                                                    "cloudformation",
                                                                    region.toString())))
                                   .build();
    }

    private CloudFormationClient getCloudFormationClient(String executionRole) {

        logger.info("Creating cloudformation client with role arn=" + executionRole);

        Credentials credentials = stsClient
                .assumeRole(AssumeRoleRequest.builder()
                                             .roleArn(executionRole)
                                             .roleSessionName("AttiniCfnDeploy")
                                             .build())
                .credentials();

        return CloudFormationClient.builder()
                                   .credentialsProvider(StaticCredentialsProvider.create(
                                           AwsSessionCredentials.create(
                                                   credentials.accessKeyId(),
                                                   credentials.secretAccessKey(),
                                                   credentials.sessionToken())))
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(
                                           URI.create(String.format("https://%s.%s.amazonaws.com",
                                                                    "cloudformation",
                                                                    environmentVariables.getRegion())))
                                   .build();
    }

    private CloudFormationClient getCloudFormationClient(Region region, String executionRole) {

        logger.info("Creating cloudformation client with role arn=" + executionRole + " and region=" + region);


        Credentials credentials = stsClient
                .assumeRole(AssumeRoleRequest.builder()
                                             .roleArn(executionRole)
                                             .roleSessionName("AttiniCfnDeploy")
                                             .build())
                .credentials();

        return CloudFormationClient.builder()
                                   .region(region)
                                   .credentialsProvider(StaticCredentialsProvider.create(
                                           AwsSessionCredentials.create(
                                                   credentials.accessKeyId(),
                                                   credentials.secretAccessKey(),
                                                   credentials.sessionToken())))
                                   .overrideConfiguration(createClientOverrideConfiguration())
                                   .httpClient(UrlConnectionHttpClient.builder().build())
                                   .endpointOverride(URI.create(String.format("https://%s.%s.amazonaws.com",
                                                                              "cloudformation",
                                                                              region.toString())))
                                   .build();
    }

    @EqualsAndHashCode
    public static class GetCloudFormationClientRequest {
        private final String region;
        private final String executionRoleArn;

        private GetCloudFormationClientRequest(Builder builder) {
            this.region = builder.region;
            this.executionRoleArn = builder.executionRoleArn;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Optional<String> getRegion() {
            return Optional.ofNullable(region);
        }

        public Optional<String> getExecutionRoleArn() {
            return Optional.ofNullable(executionRoleArn);
        }


        public static class Builder {
            private String region;
            private String executionRoleArn;

            private Builder() {
            }

            public Builder setRegion(String region) {
                this.region = region;
                return this;
            }

            public Builder setExecutionRoleArn(String executionRoleArn) {
                this.executionRoleArn = executionRoleArn;
                return this;
            }

            public GetCloudFormationClientRequest build() {
                return new GetCloudFormationClientRequest(this);
            }
        }
    }
}
