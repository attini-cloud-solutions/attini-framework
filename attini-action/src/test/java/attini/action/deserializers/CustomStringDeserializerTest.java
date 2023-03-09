package attini.action.deserializers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringDeserializer;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
public class CustomStringDeserializerTest {

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void shouldDeserializeSuccessfully() {
        Map<String, String> value = Map.of("value", "test-value");

        assertEquals(value.get("value"), objectMapper.convertValue(value, TestRecord.class).value());

    }

    @Test
    public void shouldDeserializeSuccessfully_integer() {
        Map<String, Integer> value = Map.of("value", 123232);

        assertEquals(String.valueOf(value.get("value")), objectMapper.convertValue(value, TestRecord.class).value());
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

    private record TestRecord(@JsonDeserialize(using = CustomStringDeserializer.class) String value){

    }

}
