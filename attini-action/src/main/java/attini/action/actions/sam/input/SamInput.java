package attini.action.actions.sam.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.domain.DeployOriginData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SamInput(Map<String, Object> output,
                       @JsonProperty("Properties") SamProperties properties,
                       @JsonProperty("deploymentPlanExecutionMetadata") DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata,
                       @JsonProperty("deploymentOriginData") DeployOriginData deployOriginData,
                       Map<String, Map<String,String>> dependencies,
                       Map<String, Object> customData,
                       Map<String, String> stackParameters,
                       Map<String, Object> appConfig) {
}
