package attini.action.actions.sam.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SamProperties(@JsonProperty("Parameters") Map<String, String> parameters,
                            @JsonProperty("Runner") String runner,
                            @JsonProperty("Project") Project project) {
}
