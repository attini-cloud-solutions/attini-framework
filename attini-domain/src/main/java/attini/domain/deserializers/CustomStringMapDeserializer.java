package attini.domain.deserializers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class CustomStringMapDeserializer extends StdDeserializer<Map<String, String>> {

    public CustomStringMapDeserializer() {
        this(null);
    }

    protected CustomStringMapDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Map<String, String> deserialize(JsonParser p,
                                           DeserializationContext ctxt) throws IOException {

        JsonNode jsonNode = p.readValueAsTree();

        if (!jsonNode.isObject()) {
            throw new AttiniDeserializationException("Field named " + p.getCurrentName() + " should be an Object. Current type is: " + StringUtils.capitalize(
                    jsonNode.getNodeType().name().toLowerCase()) + ", current value is: " + jsonNode);
        }


        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonNode.fields(), Spliterator.ORDERED),
                                    false)
                            .collect(Collectors.toMap(Map.Entry::getKey, o -> {
                                if (!o.getValue().isValueNode()) {
                                    try {
                                        throw new AttiniDeserializationException("Field named " + o.getKey() + " in " + p.getCurrentName() + " should be a String or a Number. Current type is: " + StringUtils.capitalize(
                                                o.getValue()
                                                 .getNodeType()
                                                 .name()
                                                 .toLowerCase()) + ", current value is: " + o.getValue());
                                    } catch (IOException e) {
                                        throw new UncheckedIOException(e);
                                    }

                                }
                                return o.getValue().asText();
                            }));
    }

}
