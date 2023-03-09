package attini.domain;

import java.util.Objects;

public class DistributionContextImpl implements DistributionContext{

    private final ObjectIdentifier objectIdentifier;
    private final Environment environment;
    private final DistributionName distributionName;
    private final DistributionId distributionId;

    private DistributionContextImpl(Builder builder) {
        objectIdentifier = builder.objectIdentifier;
        environment = builder.environment;
        distributionName = builder.distributionName;
        distributionId = builder.distributionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public DistributionName getDistributionName() {
        return distributionName;
    }

    @Override
    public DistributionId getDistributionId() {
        return distributionId;
    }





    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributionContextImpl that = (DistributionContextImpl) o;
        return Objects.equals(objectIdentifier, that.objectIdentifier) && Objects.equals(environment,
                                                                                         that.environment) && Objects.equals(
                distributionName,
                that.distributionName) && Objects.equals(distributionId, that.distributionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectIdentifier, environment, distributionName, distributionId);
    }

    @Override
    public String toString() {
        return "DistributionContextImpl{" +
               "objectIdentifier=" + objectIdentifier +
               ", environment=" + environment +
               ", distributionName=" + distributionName +
               ", distributionId=" + distributionId +
               '}';
    }

    public static final class Builder {
        private ObjectIdentifier objectIdentifier;
        private Environment environment;
        private DistributionName distributionName;
        private DistributionId distributionId;

        private Builder() {
        }

        public Builder objectIdentifier(ObjectIdentifier val) {
            objectIdentifier = val;
            return this;
        }

        public Builder environment(Environment val) {
            environment = val;
            return this;
        }

        public Builder distributionName(DistributionName val) {
            distributionName = val;
            return this;
        }

        public Builder distributionId(DistributionId val) {
            distributionId = val;
            return this;
        }

        public DistributionContextImpl build() {
            return new DistributionContextImpl(this);
        }
    }
}
