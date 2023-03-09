package attini.deploy.origin.deploystack;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.ObjectIdentifier;

public class DeployData {

    private final String deployName;
    private final Instant deployTime;
    private final DistributionId distributionId;
    private final String deploymentSourceBucket;
    private final ObjectIdentifier objectIdentifier;
    private final String errorMessage;

    private DeployData(Builder builder) {
        this.deployName = requireNonNull(builder.deployName, "deployName");
        this.deployTime = requireNonNull(builder.deployTime, "deployTime");
        this.distributionId = requireNonNull(builder.distributionId, "distributionId");
        this.deploymentSourceBucket = requireNonNull(builder.deploymentSourceBucket, "deploymentSourceBucket");
        this.objectIdentifier = requireNonNull(builder.objectIdentifier, "objectIdentifier");
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getDeployName() {
        return deployName;
    }

    public Instant getDeployTime() {
        return deployTime;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getDeploymentSourceBucket() {
        return deploymentSourceBucket;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    @Override
    public String toString() {
        return "DeployData{" +
               "deployName='" + deployName + '\'' +
               ", deployTime=" + deployTime +
               ", distributionId='" + distributionId + '\'' +
               ", deploymentSourceBucket='" + deploymentSourceBucket + '\'' +
               '}';
    }
    public static class Builder {
        private String deployName;
        private Instant deployTime;
        private DistributionId distributionId;
        private String deploymentSourceBucket;
        private ObjectIdentifier objectIdentifier;
        private String errorMessage;

        private Builder() {
        }

        public Builder setDeployName(String deployName) {
            this.deployName = deployName;
            return this;
        }

        public Builder setDeployTime(Instant deployTime) {
            this.deployTime = deployTime;
            return this;
        }

        public Builder setDistributionId(DistributionId distributionId) {
            this.distributionId = distributionId;
            return this;
        }

        public Builder setDeploymentSourceBucket(String deploymentSourceBucket) {
            this.deploymentSourceBucket = deploymentSourceBucket;
            return this;
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setErrorMessage(String errorMessage){
            this.errorMessage = errorMessage;
            return this;
        }

        public DeployData build() {
            return new DeployData(this);
        }
    }
}
