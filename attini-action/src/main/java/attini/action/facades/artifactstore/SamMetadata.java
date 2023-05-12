package attini.action.facades.artifactstore;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SamMetadata(@JsonProperty("path") String path,
                          @JsonProperty("buildDir") String buildDir,
                          @JsonProperty("template") String template,
                          @JsonProperty("stepName") String stepName) {
}
