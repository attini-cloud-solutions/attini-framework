package attini.domain.deserializers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomStringListDeserializer extends StdDeserializer<List<String>> {

    public CustomStringListDeserializer() {
        this(null);
    }

    protected CustomStringListDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<String> deserialize(JsonParser p,
                                    DeserializationContext ctxt) throws IOException {

        JsonNode jsonNode = p.readValueAsTree();

        if (!jsonNode.isArray()) {
            throw new AttiniDeserializationException("Field named " + p.getCurrentName() + " should be a List/Array. Current type is: " + StringUtils.capitalize(
                    jsonNode.getNodeType().name().toLowerCase()) + ", current value is: " + jsonNode);
        }


        return StreamSupport.stream(jsonNode.spliterator(), false).map(node -> {
            if (!node.isValueNode()) {
                try {
                    throw new AttiniDeserializationException("Values in list " + p.getCurrentName() + " should be a String or a Number. Current type is: " + StringUtils.capitalize(
                            node.getNodeType()
                                .name()
                                .toLowerCase()) + ", current value is: " + node);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

            }
            return node.asText();
        }).collect(Collectors.toList());
    }

}
