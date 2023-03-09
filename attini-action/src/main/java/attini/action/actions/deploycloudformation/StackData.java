/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation;

import java.util.Objects;

import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class StackData {

    private final StackConfiguration stackConfiguration;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final Environment environment;
    private final ObjectIdentifier objectIdentifier;
    private final DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata;
    private final ClientRequestToken clientRequestToken;

    private StackData(Builder builder) {
        this.stackConfiguration = builder.stackConfiguration;
        this.distributionId = builder.distributionId;
        this.distributionName = builder.distributionName;
        this.environment = builder.environment;
        this.objectIdentifier = builder.objectIdentifier;
        DeploymentPlanExecutionMetadata executionMetadata = builder.deploymentPlanExecutionMetadata;
        this.deploymentPlanExecutionMetadata = executionMetadata;
        this.clientRequestToken = ClientRequestToken.create(executionMetadata.executionArn());
    }

    public static Builder builder() {
        return new Builder();
    }


    public StackConfiguration getStackConfiguration() {
        return stackConfiguration;
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

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public DeploymentPlanExecutionMetadata getDeploymentPlanExecutionMetadata() {
        return deploymentPlanExecutionMetadata;
    }

    public ClientRequestToken getClientRequestToken() {
        return clientRequestToken;
    }


    public static class Builder {
        private StackConfiguration stackConfiguration;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private Environment environment;
        private ObjectIdentifier objectIdentifier;
        private DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata;
        private Builder() {
        }

        public Builder setStackConfiguration(StackConfiguration stackConfiguration) {
            this.stackConfiguration = stackConfiguration;
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

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier) {
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setDeploymentPlanExecutionMetadata(DeploymentPlanExecutionMetadata deploymentPlanExecutionMetadata) {
            this.deploymentPlanExecutionMetadata = deploymentPlanExecutionMetadata;
            return this;
        }

        public StackData build() {
            return new StackData(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackData stackData = (StackData) o;
        return Objects.equals(stackConfiguration, stackData.stackConfiguration) && Objects.equals(
                distributionId,
                stackData.distributionId) && Objects.equals(distributionName,
                                                            stackData.distributionName) && Objects.equals(
                environment,
                stackData.environment) && Objects.equals(objectIdentifier,
                                                         stackData.objectIdentifier) && Objects.equals(
                deploymentPlanExecutionMetadata,
                stackData.deploymentPlanExecutionMetadata) && Objects.equals(
                clientRequestToken,
                stackData.clientRequestToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackConfiguration,
                            distributionId,
                            distributionName,
                            environment,
                            objectIdentifier,
                            deploymentPlanExecutionMetadata,
                            clientRequestToken);
    }

    @Override
    public String toString() {
        return "StackData{" +
               "stackConfiguration=" + stackConfiguration +
               ", distributionId=" + distributionId +
               ", distributionName=" + distributionName +
               ", environment=" + environment +
               ", objectIdentifier='" + objectIdentifier + '\'' +
               ", deploymentPlanExecutionMetadata=" + deploymentPlanExecutionMetadata +
               ", clientRequestToken='" + clientRequestToken + '\'' +
               '}';
    }
}
