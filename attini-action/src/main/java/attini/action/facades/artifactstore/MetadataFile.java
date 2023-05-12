package attini.action.facades.artifactstore;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record MetadataFile(@JsonProperty("samProjects") List<SamMetadata> samProjects) {
}
