package attini.action.facades.deployorigin;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import attini.domain.DistributionName;
import attini.domain.Environment;

public final class DeploymentName {
    private final Environment environment;
    private final DistributionName distributionName;
    private final String deploymentName;


    private DeploymentName(Environment environment, DistributionName distributionName) {
        this.environment = requireNonNull(environment, "environment");
        this.distributionName = requireNonNull(distributionName, "distributionName");
        this.deploymentName = "%s-%s".formatted(environment.asString(), distributionName.asString());
    }

    public String deploymentName() {
        return deploymentName;
    }

    public Environment environment() {
        return environment;
    }

    public DistributionName distributionName() {
        return distributionName;
    }

    public static DeploymentName create(Environment environment, DistributionName distributionName){
        return new DeploymentName(environment, distributionName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DeploymentName) obj;
        return Objects.equals(this.environment, that.environment) &&
               Objects.equals(this.distributionName, that.distributionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, distributionName);
    }

    @Override
    public String toString() {
        return "DeploymentName[" +
               "environment=" + environment + ", " +
               "distributionName=" + distributionName + ']';
    }




}
