package attini.action.facades.stepfunction;

import static java.util.Objects.requireNonNull;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class ExecutionSummery {

    private final int nrOfSteps;
    private final Environment environment;
    private final DistributionName distributionName;
    private final DistributionId distributionId;
    private final ObjectIdentifier objectIdentifier;

    private ExecutionSummery(Builder builder) {
        this.nrOfSteps = builder.nrOfSteps;
        this.environment = requireNonNull(builder.environment, "environment");
        this.distributionName = requireNonNull(builder.distributionName, "distributionName");
        this.distributionId = requireNonNull(builder.distributionId, "distributionId");
        this.objectIdentifier = requireNonNull(builder.objectIdentifier, "objectIdentifier");
    }

    public static Builder builder() {
        return new Builder();
    }


    public int getNrOfSteps() {
        return nrOfSteps;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public static class Builder {
        private Integer nrOfSteps;
        private Environment environment;
        private DistributionName distributionName;
        private DistributionId distributionId;
        private ObjectIdentifier objectIdentifier;

        private Builder() {
        }

        public Builder setNrOfSteps(int nrOfSteps) {
            this.nrOfSteps = nrOfSteps;
            return this;
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder setDistributionId(DistributionId distributionId){
            this.distributionId = distributionId;
            return this;
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier){
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public ExecutionSummery build() {
            return new ExecutionSummery(this);
        }
    }


    @Override
    public String toString() {
        return "ExecutionSummery{" +
               "nrOfSteps=" + nrOfSteps +
               ", environment='" + environment + '\'' +
               ", distributionName='" + distributionName + '\'' +
               ", distributionId='" + distributionId + '\'' +
               ", objectIdentifier='" + objectIdentifier + '\'' +
               '}';
    }
}
