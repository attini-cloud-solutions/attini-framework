package deployment.plan.transform;

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

import attini.domain.deserializers.AttiniDeserializationException;

public class StatesDeserializer extends StdDeserializer<Map<String, Map<String, Object>>> {

    public StatesDeserializer() {
        this(null);
    }

    protected StatesDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Map<String, Map<String, Object>> deserialize(JsonParser p,
                                                        DeserializationContext ctxt) throws IOException {


        JsonNode jsonNode = p.readValueAsTree();

        if (!jsonNode.isObject()) {
            throw new AttiniDeserializationException("Field named " + p.getCurrentName() + " should be an Object. Current type is: " + StringUtils.capitalize(
                    jsonNode.getNodeType().name().toLowerCase()) + ", current value is: " + jsonNode);
        }

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(jsonNode.fields(),
                                                                        Spliterator.ORDERED),
                                    false)
                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                                try {
                                    JsonParser traverse = entry.getValue()
                                                               .traverse();
                                    traverse.nextToken();
                                    return ctxt.readValue(traverse,
                                                          ctxt.constructType(Map.class));

                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }));


    }


}
