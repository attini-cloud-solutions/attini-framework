package attini.deploy.origin.s3;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import attini.deploy.origin.InitDeployEvent;
import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.AttiniConfigFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

public class TagOriginObjectService {
    private static final Logger logger = Logger.getLogger(AttiniConfigFactory.class);
    private static final String ATTINI_DISTRIBUTION_NAME_KEY = "distributionName";
    private static final String ATTINI_DISTRIBUTION_ID_KEY = "distributionId";
    private final S3Client s3Client;

    public TagOriginObjectService(S3Client s3Client) {
        this.s3Client = requireNonNull(s3Client, "s3Client");
    }

    public void tagOriginObject(InitDeployEvent event, AttiniConfig attiniConfig) {
        PutObjectTaggingRequest putObjectTaggingRequest =
                PutObjectTaggingRequest.builder()
                                       .bucket(event.getS3Bucket())
                                       .key(event.getS3Key())
                                       .tagging(Tagging.builder()
                                                       .tagSet(createOriginTags(attiniConfig))
                                                       .build())
                                       .build();

        logger.info(String.format("Setting tags to origin object: %s/%s", event.getS3Key(), event.getS3Bucket()));
        s3Client.putObjectTagging(putObjectTaggingRequest);
        logger.info("Origin object successfully tagged");
    }

    private static List<Tag> createOriginTags(AttiniConfig attiniConfig) {
        Map<String, String> hashMap = new HashMap<>();

        hashMap.put(ATTINI_DISTRIBUTION_ID_KEY, attiniConfig.getAttiniDistributionId().asString());
        hashMap.put(ATTINI_DISTRIBUTION_NAME_KEY, attiniConfig.getAttiniDistributionName().asString());

        hashMap.putAll(attiniConfig.getAttiniDistributionTags());

        return hashMap.entrySet()
                      .stream()
                      .map(entry -> toTag(entry.getKey(), entry.getValue()))
                      .toList();
    }

    private static Tag toTag(String key, String value) {
        return Tag.builder()
                  .key(key)
                  .value(value)
                  .build();
    }
}
