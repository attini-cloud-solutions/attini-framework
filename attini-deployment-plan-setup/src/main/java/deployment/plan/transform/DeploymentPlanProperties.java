package deployment.plan.transform;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringDeserializer;
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

    private final String defaultRunner;
    private final CfnString name;

    @JsonCreator
    public DeploymentPlanProperties(@JsonProperty("DeploymentPlan") DeploymentPlan deploymentPlan,
                                    @JsonProperty("Name") CfnString name,
                                    @JsonProperty("RoleArn") CfnString roleArn,
                                    @JsonProperty("DefinitionSubstitutions") Map<String, Object> definitionSubstitutions,
                                    @JsonProperty("PermissionsBoundary") CfnString permissionsBoundary,
                                    @JsonProperty("Policies") CfnString policies,
                                    @JsonProperty("PayloadDefaults") Map<String, Object> payloadDefaults,
                                    @JsonProperty("DefaultRunner")@JsonDeserialize(using = CustomStringDeserializer.class) String defaultRunner) {

        if (deploymentPlan == null) {
            throw new IllegalArgumentException(
                    "Attini::Deploy::DeploymentPlan resource is missing DeploymentPlan property");
        }
        this.name = name;
        this.defaultRunner = defaultRunner;
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

    @JsonProperty("Name")
    public Optional<CfnString> getName() {
        return Optional.ofNullable(name);
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

    @JsonProperty("DefaultRunner")
    public String getDefaultRunner() {
        return  defaultRunner == null ? "AttiniDefaultRunner" : defaultRunner;
    }

    public boolean hasCustomDefaultRunner(){
        return defaultRunner != null;
    }
}
