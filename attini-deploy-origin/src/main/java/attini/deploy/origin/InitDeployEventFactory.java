package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.ObjectIdentifier;

public class InitDeployEventFactory {

    private static final Logger logger = Logger.getLogger(InitDeployEventFactory.class);

    private final ObjectMapper mapper;

    public InitDeployEventFactory(ObjectMapper objectMapper) {
        this.mapper = requireNonNull(objectMapper, "objectMapper");
    }

    public InitDeployEvent create(Map<String, Object> input) {

        String inputString = writeMapAsJsonString(input);
        logger.info("Got event: " + inputString);

        JsonNode s3EventJson = getS3EventJson(inputString);
        String s3Key = getS3ObjectKey(s3EventJson);

        return new InitDeployEvent(getS3BucketName(s3EventJson),
                                   s3Key,
                                   ObjectIdentifier.of(s3Key + "#" + getS3ObjectVersion(s3EventJson)),
                                   getUserIdentity(s3EventJson));
    }

    private String writeMapAsJsonString(Map<String, Object> input) {
        try {
            return mapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            logger.fatal("Could not parse the given input to init deploy as a json", e);
            throw new RuntimeException(e);
        }
    }

    private JsonNode getS3EventJson(String input) {
        try {
            return mapper.readTree(input);
        } catch (JsonProcessingException e) {
            logger.fatal("Could not process the input string from init deploy event as json", e);
            throw new RuntimeException(e);
        }
    }

    private static String getS3ObjectKey(JsonNode jsonNode) {
        try {
            return jsonNode.get("Records").get(0).get("s3").get("object").get("key").asText();
        } catch (NullPointerException e) {
            logger.fatal("Could not find the s3 object key in the event, make sure its a valid s3_put event");
            throw e;
        }
    }

    private static String getUserIdentity(JsonNode jsonNode) {
        try {
            return jsonNode.get("Records").get(0).get("userIdentity").get("principalId").asText();
        } catch (NullPointerException e) {
            logger.fatal("Could not find the user identity in the event, make sure its a valid s3_put event");
            throw e;
        }
    }

    private static String getS3ObjectVersion(JsonNode jsonNode) {
        try {
            return jsonNode.get("Records").get(0).get("s3").get("object").get("versionId").asText();
        } catch (NullPointerException e) {
            logger.fatal("Could not find the s3 object version in the event, make sure its a valid s3_put event");
            throw e;
        }
    }


    private static String getS3BucketName(JsonNode jsonNode) {
        try {
            return jsonNode.get("Records").get(0).get("s3").get("bucket").get("name").asText();
        } catch (NullPointerException e) {
            logger.fatal("Could not find the s3 bucket name in the event, make sure its a valid s3_put event");
            throw e;
        }
    }

}
