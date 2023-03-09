package attini.action.deserializers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringListDeserializer;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
public class CustomStringListDeserializerTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void shouldDeserializeSuccessfully() {
        Map<String, List<String>> value = Map.of("value", List.of("test", "test2"));

        assertEquals(value.get("value"), objectMapper.convertValue(value, TestRecord.class).value());

    }

    @Test
    public void shouldDeserializeSuccessfully_integer() {
        Map<String, List<Object>> value = Map.of("value",  List.of("test", 11111));

        assertEquals(value.get("value").stream().map(String::valueOf).collect(Collectors.toList()), objectMapper.convertValue(value, TestRecord.class).value());
    }

    @Test
    public void shouldFail_map() {
        Map<String, Object> value = Map.of("value", Map.of("key", "value"));

        assertThrows(IllegalArgumentException.class, () -> objectMapper.convertValue(value, TestRecord.class));

    }

    @Test
    public void shouldFail_string() {
        Map<String, Object> value = Map.of("value", "test");

        assertThrows(IllegalArgumentException.class, () -> objectMapper.convertValue(value, TestRecord.class));

    }

    private record TestRecord(@JsonDeserialize(using = CustomStringListDeserializer.class) List<String> value){

    }

}
