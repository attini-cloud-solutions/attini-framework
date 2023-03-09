package attini.action.actions.runner.input;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringListDeserializer;
import attini.domain.deserializers.CustomStringMapDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record RunnerProperties(
        @JsonProperty("Commands") @JsonDeserialize(using = CustomStringListDeserializer.class) List<String> commands,
        @JsonProperty("Runner") String runner,
        @JsonProperty("Environment") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String,String> environment) {


}
