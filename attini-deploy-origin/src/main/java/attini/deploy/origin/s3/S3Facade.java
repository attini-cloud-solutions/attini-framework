package attini.deploy.origin.s3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import com.google.common.collect.Lists;

import attini.deploy.origin.PublishDistributionException;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public class S3Facade {

    private static final Logger logger = Logger.getLogger(S3Facade.class);
    private static final String ILLEGAL_CHARS = "*[#%{}`~<>|^ &;?$,+=@]";
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;

    public S3Facade(S3Client s3Client, S3AsyncClient s3AsyncClient) {
        this.s3Client = s3Client;
        this.s3AsyncClient = s3AsyncClient;
    }

    public byte[] downloadS3File(String bucket, String key) {

        logger.info(String.format("Downloading distribution from s3 %s/%s", bucket, key));
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(bucket)
                                                            .key(key)
                                                            .build();

        ResponseBytes<GetObjectResponse> object = s3Client.getObject(
                getObjectRequest,
                ResponseTransformer.toBytes());
        return object.asByteArray();

    }

    public void uploadDirectory(Path localPath,
                                String s3Bucket,
                                String s3Prefix,
                                DistributionName distributionName,
                                DistributionId distributionId) {
        try (Stream<Path> walk = Files.walk(localPath)) {
            List<String> files = walk.filter(Files::isRegularFile)
                                     .map(Path::toString)
                                     .toList();

            files.forEach(s -> {
                if (StringUtils.containsAny(s, ILLEGAL_CHARS)) {
                    throw new PublishDistributionException(distributionName, distributionId,
                                                           "Could not publish artifact, illegal character in file = " + s);
                }
            });

            List<List<String>> partitions = Lists.partition(files, 700);

            logger.info("Uploading distribution to artifact store");

            for (List<String> partition : partitions){
                partition.parallelStream()
                     .map(uploadDistribution(localPath, s3Bucket, s3Prefix))
                     .reduce((f1, f2) -> f1.thenCombine(f2, (unused, unused2) -> unused))
                     .ifPresent(CompletableFuture::join);
                logger.info("Uploaded distribution partition to artifact store");
            }

            logger.info("Done Uploading distribution to artifact store");


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public void uploadFile(byte[] file, String s3Bucket, String s3Prefix, String fileName) {
        s3Client.putObject(PutObjectRequest.builder()
                                           .bucket(s3Bucket)
                                           .key(s3Prefix + "/" + fileName)
                                           .build(),
                           RequestBody.fromBytes(file));

    }

    private Function<String, CompletableFuture<PutObjectResponse>> uploadDistribution(Path localPath,
                                                String s3Bucket,
                                                String s3Prefix) {
        return path -> {
            String fileName = path.substring(localPath.toString().length());

            return s3AsyncClient.putObject(
                    PutObjectRequest.builder()
                                    .bucket(s3Bucket)
                                    .key(s3Prefix + fileName)
                                    .build(),
                    AsyncRequestBody.fromFile(Paths.get(path)));
        };
    }
}
