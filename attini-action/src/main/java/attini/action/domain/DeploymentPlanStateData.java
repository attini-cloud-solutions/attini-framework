package attini.action.domain;

import static java.util.Objects.requireNonNull;

import attini.domain.ObjectIdentifier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class DeploymentPlanStateData {
    private final String deployOriginSourceName;
    private final ObjectIdentifier objectIdentifier;

    private final String payloadDefaults;

    public DeploymentPlanStateData(String deployOriginSourceName, ObjectIdentifier objectIdentifier, String payloadDefaults) {
        this.deployOriginSourceName = requireNonNull(deployOriginSourceName, "deployOriginSourceName");
        this.objectIdentifier = requireNonNull(objectIdentifier, "objectIdentifier");
        this.payloadDefaults = requireNonNull(payloadDefaults, "payloadDefaults");
    }

    public String getDeployOriginSourceName() {
        return deployOriginSourceName;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getPayloadDefaults() {
        return payloadDefaults;
    }
}
