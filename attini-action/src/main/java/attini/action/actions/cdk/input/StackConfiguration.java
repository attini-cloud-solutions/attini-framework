package attini.action.actions.cdk.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringDeserializer;
import attini.domain.deserializers.CustomStringMapDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record StackConfiguration(
        @JsonProperty("StackName") @JsonDeserialize(using = CustomStringDeserializer.class) String stackName,
        @JsonProperty("Parameters") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> parameters) {
}
