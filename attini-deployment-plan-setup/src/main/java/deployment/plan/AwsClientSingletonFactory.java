/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan;

import java.net.URI;
import java.time.Duration;

import org.jboss.logging.Logger;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.ec2.Ec2Client;


//TODO Singleton implementation is not required here and can be replaced
public class AwsClientSingletonFactory {
    private static final Logger logger = Logger.getLogger(String.valueOf(Object.class));
    private static ClientOverrideConfiguration clientOverrideConfiguration;
    private static CloudFormationClient cfnClient;
    private static DynamoDbClient dynamoDbClient;
    private static Ec2Client ec2Client;


    public static CloudFormationClient getCfnClient(){
        if (cfnClient == null){
            logger.debug("Creating CfnClient");
            cfnClient = CloudFormationClient.builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .overrideConfiguration(createClientOverrideConfiguration())
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .endpointOverride(
                            getAwsServiceEndpoint("cloudFormation")
                    )
                    .build();
        } else {
            logger.debug("Re-using CfnClient");
        }
        return cfnClient;
    }

    public static DynamoDbClient getDynamoDbClient(){
        if(dynamoDbClient == null){
            logger.debug("Creating DynamoDbClient");
            dynamoDbClient = DynamoDbClient.builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .overrideConfiguration(createClientOverrideConfiguration())
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .endpointOverride(
                            getAwsServiceEndpoint("dynamodb")
                    )
                    .build();
        } else {
            logger.debug("Re-using DynamoDbClient");
        }
        return dynamoDbClient;
    }

    public static Ec2Client getEc2Client(){
        if(ec2Client == null){
            logger.debug("Creating DynamoDbClient");
            ec2Client = Ec2Client.builder()
                                      .region(Region.of(System.getenv("AWS_REGION")))
                                      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                      .overrideConfiguration(createClientOverrideConfiguration())
                                      .httpClient(UrlConnectionHttpClient.builder().build())
                                      .endpointOverride(
                                                   getAwsServiceEndpoint("ec2")
                                           )
                                      .build();
        } else {
            logger.debug("Re-using DynamoDbClient");
        }
        return ec2Client;
    }


    private static ClientOverrideConfiguration createClientOverrideConfiguration(){
       if (clientOverrideConfiguration == null) {
           logger.debug("Creating ClientOverrideConfiguration");
           clientOverrideConfiguration = ClientOverrideConfiguration.builder()
                   .apiCallTimeout(Duration.ofSeconds(240))
                   .apiCallAttemptTimeout(Duration.ofSeconds(30))
                   .retryPolicy(RetryPolicy.builder()
                           .numRetries(20)
                           .build())
                   .build();
       } else {
           logger.debug("Re-using ClientOverrideConfiguration");
       }
       return clientOverrideConfiguration;
    }

    private static URI getAwsServiceEndpoint(String service){
        return URI.create(String.format("https://%s.%s.amazonaws.com", service, System.getenv("AWS_REGION")));
    }
}
