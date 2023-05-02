package attini.action.deserializers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringMapDeserializer;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
public class CustomStringMapDeserializerTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void shouldDeserializeSuccessfully() {
        Map<String, Object> content = Map.of("parameters",
                                             Map.of("test", "test"),
                                             "variables",
                                             Map.of("test2", "test2"));

        TestRecord testRecord = objectMapper.convertValue(content, TestRecord.class);

        assertEquals(content.get("parameters"), testRecord.parameters());
        assertEquals(content.get("variables"), testRecord.variables());

    }

    @Test
    public void shouldDeserializeSuccessfully_allow_integer() {
        Map<String, Integer> params = Map.of("test", 12322);
        Map<String, Object> content = Map.of("parameters",
                                             params,
                                             "variables",
                                             Map.of("test2", "test2"));

        TestRecord testRecord = objectMapper.convertValue(content, TestRecord.class);

        assertEquals(String.valueOf(params.get("test")), testRecord.parameters().get("test"));
        assertEquals(content.get("variables"), testRecord.variables());

    }

    @Test
    public void shouldDeserializeSuccessfully_allow_boolean() {
        Map<String, Boolean> params = Map.of("test", true);
        Map<String, Object> content = Map.of("parameters",
                                             params,
                                             "variables",
                                             Map.of("test2", "test2"));

        TestRecord testRecord = objectMapper.convertValue(content, TestRecord.class);

        assertEquals(String.valueOf(params.get("test")), testRecord.parameters().get("test"));
        assertEquals(content.get("variables"), testRecord.variables());

    }

    @Test
    public void shouldDeserializeSuccessfully_allow_empty() {
        Map<String, Object> content = Map.of("parameters",
                                             Map.of("test", "test"));

        TestRecord testRecord = objectMapper.convertValue(content, TestRecord.class);

        assertEquals(content.get("parameters"), testRecord.parameters());
        assertNull(testRecord.variables());
    }

    @Test
    public void shouldFailOnIllegalType_string() {
        Map<String, Object> content = Map.of("parameters", "test");

        assertThrows(IllegalArgumentException.class,() -> objectMapper.convertValue(content, TestRecord.class));
    }

    @Test
    public void shouldFailOnIllegalType_integer() {
        Map<String, Object> content = Map.of("parameters", 2232323);

        assertThrows(IllegalArgumentException.class,() -> objectMapper.convertValue(content, TestRecord.class));
    }

    @Test
    public void shouldFailOnIllegalType_list() {
        Map<String, Object> content = Map.of("parameters", List.of("1232", "test"));

        assertThrows(IllegalArgumentException.class,() -> objectMapper.convertValue(content, TestRecord.class));
    }

    @Test
    public void shouldFailOnMapWithIllegalValue_object() {
        Map<String, Object> content = Map.of("parameters", Map.of("parameters",
                                                                  Map.of("test", Map.of("test2", "test2"))));

        assertThrows(IllegalArgumentException.class,() -> objectMapper.convertValue(content, TestRecord.class));
    }

    @Test
    public void shouldFailOnMapWithIllegalValue_list() {
        Map<String, Object> content = Map.of("parameters", Map.of("parameters",
                                                                  Map.of("test", List.of("test"))));

        assertThrows(IllegalArgumentException.class,() -> objectMapper.convertValue(content, TestRecord.class));
    }


    public record TestRecord(@JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> parameters,
                             @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> variables) {
    }

}
