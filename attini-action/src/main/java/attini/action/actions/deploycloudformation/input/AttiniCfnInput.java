package attini.action.actions.deploycloudformation.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import attini.domain.DeployOriginData;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AttiniCfnInput(@JsonProperty("Properties") CfnConfig cfnConfig,
                             @JsonProperty("attiniActionType") String attiniActionType,
                             @JsonProperty("deploymentPlanExecutionMetadata")
                             DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata,
                             @JsonProperty("output") Map<String, Object> output,
                             @JsonProperty("deploymentOriginData") DeployOriginData deploymentOriginData,
                             @JsonProperty("customData") Map<String, Object> customData) {
}
