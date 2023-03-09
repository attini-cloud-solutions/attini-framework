package attini.domain.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;

public class AttiniJsonProvider extends JacksonJsonProvider {

    public AttiniJsonProvider() {
        super(new ObjectMapper(new YAMLFactory()));
    }

    @Override
    public Object parse(String json) throws InvalidJsonException {
        try {
            JsonNode jsonNode = objectReader.readTree(json);
            if (jsonNode.isValueNode()){
                throw new InvalidJsonException("Could not read document, must be either valid json or yaml");
            }

            return objectReader.treeToValue(jsonNode, Object.class);
        } catch (IOException e) {
            throw new InvalidJsonException(e, json);
        }
    }

}
