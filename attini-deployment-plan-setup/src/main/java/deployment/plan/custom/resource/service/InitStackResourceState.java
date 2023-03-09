package deployment.plan.custom.resource.service;

public class InitStackResourceState {

    private final String objectIdentifier;
    private final String distributionId;
    private final String distributionName;
    private final String environment;

    private InitStackResourceState(Builder builder) {
        this.objectIdentifier = builder.objectIdentifier;
        this.distributionId = builder.distributionId;
        this.distributionName = builder.distributionName;
        this.environment = builder.environment;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getDistributionId() {
        return distributionId;
    }

    public String getDistributionName() {
        return distributionName;
    }

    public String getEnvironment() {
        return environment;
    }

    public static class Builder {
        private String objectIdentifier;
        private String distributionId;
        private String distributionName;
        private String environment;

        private Builder() {
        }

        public Builder setObjectIdentifier(String objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setDistributionId(String distributionId) {
            this.distributionId = distributionId;
            return this;
        }

        public Builder setDistributionName(String distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder setEnvironment(String environment) {
            this.environment = environment;
            return this;
        }

        public InitStackResourceState build() {
            return new InitStackResourceState(this);
        }
    }
}
