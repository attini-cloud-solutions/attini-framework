package deployment.plan.transform;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DeploymentPlanProperties {

    private final DeploymentPlan deploymentPlan;
    private final CfnString roleArn;
    private final Map<String, Object> definitionSubstitutions;
    private final CfnString permissionsBoundary;

    private final Map<String, Object> payloadDefaults;

    //Can be a List, Map, or String
    private final Object policies;

    @JsonCreator
    public DeploymentPlanProperties(@JsonProperty("DeploymentPlan") DeploymentPlan deploymentPlan,
                                    @JsonProperty("RoleArn") CfnString roleArn,
                                    @JsonProperty("DefinitionSubstitutions") Map<String, Object> definitionSubstitutions,
                                    @JsonProperty("PermissionsBoundary") CfnString permissionsBoundary,
                                    @JsonProperty("Policies") CfnString policies,
                                    @JsonProperty("PayloadDefaults") Map<String, Object> payloadDefaults) {

        if (deploymentPlan == null) {
            throw new IllegalArgumentException(
                    "Attini::Deploy::DeploymentPlan resource is missing DeploymentPlan property");
        }
        this.deploymentPlan = deploymentPlan;
        this.roleArn = roleArn;
        this.definitionSubstitutions = definitionSubstitutions;
        this.permissionsBoundary = permissionsBoundary;
        this.policies = policies;
        this.payloadDefaults = payloadDefaults;
    }

    @JsonProperty("DeploymentPlan")
    public DeploymentPlan getDeploymentPlan() {
        return deploymentPlan;
    }

    @JsonProperty("RoleArn")
    public Optional<CfnString> getRoleArn() {
        return Optional.ofNullable(roleArn);
    }

    @JsonProperty("DefinitionSubstitutions")
    public Map<String, Object> getDefinitionSubstitutions() {
        return definitionSubstitutions == null ? Collections.emptyMap() : definitionSubstitutions;
    }

    @JsonProperty("PermissionsBoundary")
    public Optional<CfnString> getPermissionsBoundary() {
        return Optional.ofNullable(permissionsBoundary);
    }

    @JsonProperty("Policies")
    public Optional<Object> getPolicies() {
        return Optional.ofNullable(policies);
    }

    @JsonProperty("PayloadDefaults")
    public Map<String, Object> getPayloadDefaults() {
        return payloadDefaults == null ? Collections.emptyMap() : payloadDefaults;
    }
}
