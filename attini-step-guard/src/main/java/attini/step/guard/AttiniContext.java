package attini.step.guard;

import static java.util.Objects.requireNonNull;

import attini.domain.DistributionContext;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import attini.step.guard.stackdata.ResourceState;

public class AttiniContext implements ResourceState, DistributionContext {

    private final Environment environment;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final ObjectIdentifier objectIdentifier;

    private AttiniContext(Builder builder) {
        this.environment = requireNonNull(builder.environment, "environment");
        this.distributionId = requireNonNull(builder.distributionId, "distributionId");
        this.distributionName = requireNonNull(builder.distributionName, "distributionName");
        this.objectIdentifier = requireNonNull(builder.objectIdentifier, "objectIdentifier");
    }

    public static Builder builder() {
        return new Builder();
    }


    public Environment getEnvironment() {
        return environment;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public static class Builder {
        private Environment environment;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private ObjectIdentifier objectIdentifier;

        private Builder() {
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDistributionId(DistributionId distributionId) {
            this.distributionId = distributionId;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public AttiniContext build() {
            return new AttiniContext(this);
        }
    }
}
