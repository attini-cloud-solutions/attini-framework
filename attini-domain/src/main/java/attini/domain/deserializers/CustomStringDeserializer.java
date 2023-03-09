package attini.domain.deserializers;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomStringDeserializer extends StdDeserializer<String> {

    public CustomStringDeserializer() {
        this(null);
    }

    protected CustomStringDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser p,
                              DeserializationContext ctxt) throws IOException {

        JsonNode jsonNode = p.readValueAsTree();

        if (!jsonNode.isValueNode()) {
            throw new AttiniDeserializationException("Field named " + p.getCurrentName() + " should be a String. Current type is: " + StringUtils.capitalize(
                    jsonNode.getNodeType().name().toLowerCase()) + ", current value is: " + jsonNode);
        }

        return jsonNode.asText();
    }

}
