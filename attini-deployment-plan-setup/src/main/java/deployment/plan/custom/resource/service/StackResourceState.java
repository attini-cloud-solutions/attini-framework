package deployment.plan.custom.resource.service;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class StackResourceState {

    private final ObjectIdentifier objectIdentifier;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final Environment environment;

    private StackResourceState(Builder builder) {
        this.objectIdentifier = builder.objectIdentifier;
        this.distributionId = builder.distributionId;
        this.distributionName = builder.distributionName;
        this.environment = builder.environment;
    }

    public static Builder builder() {
        return new Builder();
    }


    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public static class Builder {
        private ObjectIdentifier objectIdentifier;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private Environment environment;

        private Builder() {
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
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

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public StackResourceState build() {
            return new StackResourceState(this);
        }
    }
}
