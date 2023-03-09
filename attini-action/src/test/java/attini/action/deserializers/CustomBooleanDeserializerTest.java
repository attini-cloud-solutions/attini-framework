package attini.action.deserializers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomBooleanDeserializer;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
public class CustomBooleanDeserializerTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void shouldDeserializeSuccessfully_string_true() {
        Map<String, String> value = Map.of("value", "true");

        assertEquals(value.get("value"), objectMapper.convertValue(value, TestRecord.class).value());

    }

    @Test
    public void shouldDeserializeSuccessfully_string_false() {
        Map<String, String> value = Map.of("value", "false");

        assertEquals(String.valueOf(value.get("value")), objectMapper.convertValue(value, TestRecord.class).value());
    }

    @Test
    public void shouldDeserializeSuccessfully_true() {
        Map<String, Boolean> value = Map.of("value", true);

        assertEquals(String.valueOf(value.get("value")), objectMapper.convertValue(value, TestRecord.class).value());

    }

    @Test
    public void shouldDeserializeSuccessfully_false() {
        Map<String, Boolean> value = Map.of("value", false);

        assertEquals(String.valueOf(value.get("value")), objectMapper.convertValue(value, TestRecord.class).value());
    }

    @Test
    public void shouldFail_invalid_string() {
        Map<String, Object> value = Map.of("value","some-value");

        assertThrows(IllegalArgumentException.class, () -> objectMapper.convertValue(value, TestRecord.class));

    }

    @Test
    public void shouldFail_map() {
        Map<String, Object> value = Map.of("value", Map.of("key", "value"));

        assertThrows(IllegalArgumentException.class, () -> objectMapper.convertValue(value, TestRecord.class));

    }

    @Test
    public void shouldFail_list() {
        Map<String, Object> value = Map.of("value", List.of("value1", "value2"));

        assertThrows(IllegalArgumentException.class, () -> objectMapper.convertValue(value, TestRecord.class));

    }

    private record TestRecord(@JsonDeserialize(using = CustomBooleanDeserializer.class) String value){

    }

}
