package attini.action.domain;

import static java.util.Objects.requireNonNull;

import attini.action.facades.deployorigin.DeploymentName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class DeploymentPlanStateData {
    private final DeploymentName deployOriginSourceName;
    private final Environment environment;
    private final ObjectIdentifier objectIdentifier;

    private final String payloadDefaults;

    public DeploymentPlanStateData(DeploymentName deployOriginSourceName,
                                   ObjectIdentifier objectIdentifier,
                                   String payloadDefaults,
                                   Environment environment) {
        this.deployOriginSourceName = requireNonNull(deployOriginSourceName, "deployOriginSourceName");
        this.objectIdentifier = requireNonNull(objectIdentifier, "objectIdentifier");
        this.payloadDefaults = requireNonNull(payloadDefaults, "payloadDefaults");
        this.environment = requireNonNull(environment, "environment");
    }

    public DeploymentName getDeployOriginSourceName() {
        return deployOriginSourceName;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getPayloadDefaults() {
        return payloadDefaults;
    }

    public Environment getEnvironment() {
        return environment;
    }
}
