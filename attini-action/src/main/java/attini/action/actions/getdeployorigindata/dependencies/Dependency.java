package attini.action.actions.getdeployorigindata.dependencies;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Dependency(String outputUrl, String deploymentSourcePrefix) {

}
