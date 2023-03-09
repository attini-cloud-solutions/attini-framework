package deployment.plan.transform;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractRequestResponse {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    abstract JsonNode request();


    abstract  JsonNode expectedResponse();

    protected static JsonNode readValue(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}
