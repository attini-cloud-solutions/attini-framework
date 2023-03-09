package attini.domain.deserializers;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomBooleanDeserializer extends StdDeserializer<String> {

    public CustomBooleanDeserializer() {
        this(null);
    }

    protected CustomBooleanDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser p,
                              DeserializationContext ctxt) throws IOException {

        JsonNode jsonNode = p.readValueAsTree();

        if (!jsonNode.isBoolean() &&
            !jsonNode.asText().equalsIgnoreCase("true") &&
            !jsonNode.asText().equalsIgnoreCase("false")) {
            throw new AttiniDeserializationException("Field named " + p.getCurrentName() + " should be a Boolean. Current type is: " + StringUtils.capitalize(
                    jsonNode.getNodeType().name().toLowerCase()) + ", current value is: " + jsonNode);
        }

        return jsonNode.asText();
    }

}
