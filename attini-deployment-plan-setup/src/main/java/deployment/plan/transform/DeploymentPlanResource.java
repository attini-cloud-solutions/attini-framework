package deployment.plan.transform;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;


@RegisterForReflection
public class DeploymentPlanResource {
    private final Map<String, Object> metadata;
    private final DeploymentPlanProperties deploymentPlanProperties;


    @JsonCreator
    public DeploymentPlanResource(@JsonProperty("Metadata")  Map<String, Object> metadata,
                                  @JsonProperty("Properties") DeploymentPlanProperties deploymentPlanProperties) {
        this.metadata = metadata;
        if (deploymentPlanProperties == null) {
            throw new IllegalArgumentException("No DeploymentPlan property is present in the Deployment plan config");
        }
        this.deploymentPlanProperties = deploymentPlanProperties;
    }

    @JsonProperty("Metadata")
    public Map<String, Object> getMetadata() {
        return metadata == null ? Collections.emptyMap() : metadata;
    }

    @JsonProperty("Properties")
    public DeploymentPlanProperties getDeploymentPlanProperties() {
        return deploymentPlanProperties;
    }
}
