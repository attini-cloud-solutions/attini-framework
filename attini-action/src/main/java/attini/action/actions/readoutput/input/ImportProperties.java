package attini.action.actions.readoutput.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringDeserializer;
import attini.domain.deserializers.CustomStringMapDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ImportProperties(
        @JsonProperty("SourceType") @JsonDeserialize(using = CustomStringDeserializer.class) String sourceType,
        @JsonProperty("Source") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> source,
        @JsonProperty("Mapping") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> mapping,
        @JsonProperty("ExecutionRoleArn") @JsonDeserialize(using = CustomStringDeserializer.class) String executionRole) {
}
