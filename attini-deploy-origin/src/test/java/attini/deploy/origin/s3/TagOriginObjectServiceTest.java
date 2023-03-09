package attini.deploy.origin.s3;

import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.deploy.origin.InitDeployEvent;
import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.AttiniConfigTestBuilder;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

@ExtendWith(MockitoExtension.class)
class TagOriginObjectServiceTest {

    public static final String BUCKET = "testBucket";
    public static final String KEY = "dev/platform";
    private static final ObjectIdentifier SOME_OBJECT_IDENTIFIER = ObjectIdentifier.of("someObjectIdentifier");
    private static final String SOME_USER = "pelle.the.boss";


    @Mock
    S3Client s3Client;

    TagOriginObjectService tagOriginObjectService;

    @BeforeEach
    void setUp() {
        tagOriginObjectService = new TagOriginObjectService(s3Client);
    }

    @Test
    void tagOriginObject() {
        AttiniConfig config = AttiniConfigTestBuilder.aConfig().build();
        InitDeployEvent event = new InitDeployEvent(BUCKET, KEY, SOME_OBJECT_IDENTIFIER, SOME_USER);

        tagOriginObjectService.tagOriginObject(event, config);
        verify(s3Client).putObjectTagging(PutObjectTaggingRequest.builder()
                                                                 .bucket(BUCKET)
                                                                 .key(KEY)
                                                                 .tagging(Tagging.builder()
                                                                                 .tagSet(Tag.builder()
                                                                                            .key("distributionId")
                                                                                            .value(config.getAttiniDistributionId().asString())
                                                                                            .build(),
                                                                                         Tag.builder()
                                                                                            .key("distributionName")
                                                                                            .value(config.getAttiniDistributionName().asString())
                                                                                            .build())
                                                                                 .build())
                                                                 .build());
    }

    @Test
    void tagOriginObject_shouldSetDistributionTags() {
        AttiniConfig config = AttiniConfigTestBuilder.aConfig().distributionTags(Map.of("tagKey", "tagValue")).build();

        InitDeployEvent event = new InitDeployEvent(BUCKET, KEY, SOME_OBJECT_IDENTIFIER, SOME_USER);

        tagOriginObjectService.tagOriginObject(event, config);
        verify(s3Client).putObjectTagging(PutObjectTaggingRequest.builder()
                                                                 .bucket(BUCKET)
                                                                 .key(KEY)
                                                                 .tagging(Tagging.builder()
                                                                                 .tagSet(Tag.builder()
                                                                                            .key("distributionId")
                                                                                            .value(config.getAttiniDistributionId().asString())
                                                                                            .build(),
                                                                                         Tag.builder()
                                                                                            .key("distributionName")
                                                                                            .value(config.getAttiniDistributionName().asString())
                                                                                            .build(),
                                                                                         Tag.builder()
                                                                                            .key("tagKey")
                                                                                            .value("tagValue")
                                                                                            .build())
                                                                                 .build())
                                                                 .build());
    }
}
