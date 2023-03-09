package deployment.plan.transform;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.ToString;

/**
 * Wrapper class for Strings that can represent a String or another Cfn pseudo parameter reference.
 *
 */
@RegisterForReflection
@ToString
public class CfnString {

    private final JsonNode value;

    private CfnString(JsonNode value) {
        this.value = requireNonNull(value, "value");
    }

    @JsonValue
    public JsonNode getUncheckedValue() {
        return value;
    }

    @JsonCreator
    public static CfnString create(JsonNode jsonNode){
        if (jsonNode.isMissingNode()){
            return null;
        }
        return new CfnString(jsonNode);
    }

    public static CfnString create(String value){
        return CfnString.create(TextNode.valueOf(value));
    }
    /**
     *
     * @return false if the object contains a not yet substituted pseudo parameter reference.
     */
    public boolean isString(){
       return value.isTextual();
    }

    public String asString(){
        return value.textValue();
    }


}
