package attini.action;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

public class S3ClientFactory {

    private static final Logger logger = Logger.getLogger(S3ClientFactory.class);

    private final S3Client s3Client;
    private final StsClient stsClient;

    public S3ClientFactory(S3Client s3Client, StsClient stsClient) {
        this.s3Client = requireNonNull(s3Client, "s3Client");
        this.stsClient = requireNonNull(stsClient, "stsClient");
    }

    public S3Client getClient(String executionRole){
        if (executionRole != null){
            logger.info("Creating S3 client with custom execution role");
            Credentials credentials = stsClient
                    .assumeRole(AssumeRoleRequest.builder()
                                                 .roleArn(executionRole)
                                                 .roleSessionName("AttiniDeploy")
                                                 .build())
                    .credentials();

            return S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(
                                   AwsSessionCredentials.create(
                                           credentials.accessKeyId(),
                                           credentials.secretAccessKey(),
                                           credentials.sessionToken())))
                           .build();


        }
        return s3Client;
    }
}
