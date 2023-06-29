package attini.action.actions.sam.input;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Project(@JsonProperty("Path") String path, @JsonProperty("BuildDir") String buildDir, @JsonProperty("Template") String template) {
}
