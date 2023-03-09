package attini.action.actions.readoutput.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import attini.domain.DeployOriginData;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record ImportInput(
        @JsonProperty("Properties") ImportProperties properties,
        @JsonProperty("deploymentPlanExecutionMetadata") DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata,
        @JsonProperty("deploymentOriginData") DeployOriginData deployOriginData,
        Map<String, Map<String, String>> dependencies,
        Map<String, Object> customData,
        Map<String, String> stackParameters) {
}
