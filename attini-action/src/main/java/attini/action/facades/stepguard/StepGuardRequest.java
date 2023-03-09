/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.facades.stepguard;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import attini.action.domain.DesiredState;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
class StepGuardRequest {
    private final String stackName;
    private final String resourceStatus;
    private final String logicalResourceId;
    private final String resourceType;
    private final String region;
    private final String executionRoleArn;
    private final String stackId;
    private final String sfnResponseToken;
    private final String stepName;
    private final String outputPath;
    private final Environment environment;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final ObjectIdentifier objectIdentifier;
    private final String clientRequestToken;
    private final String desiredState;


    private StepGuardRequest(Builder builder) {
        this.stackName = requireNonNull(builder.stackName, "stackName");
        this.resourceStatus = requireNonNull(builder.resourceStatus, "resourceStatus");
        this.logicalResourceId = requireNonNull(builder.logicalResourceId, "logicalResourceId");
        this.resourceType = requireNonNull(builder.resourceType, "resourceType");
        this.region = builder.region;
        this.executionRoleArn = builder.executionRoleArn;
        this.stackId = builder.stackId;
        this.sfnResponseToken = builder.sfnResponseToken;
        this.stepName = requireNonNull(builder.stepName, "stepName");
        this.outputPath = builder.outputPath;
        this.environment = builder.environment;
        this.distributionId = builder.distributionId;
        this.distributionName = builder.distributionName;
        this.objectIdentifier = builder.objectIdentifier;
        this.clientRequestToken = requireNonNull(builder.clientRequestToken, "clientRequestToken");
        this.desiredState = builder.desiredState.name();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getStackName() {
        return stackName;
    }

    public String getResourceStatus() {
        return resourceStatus;
    }

    public String getLogicalResourceId() {
        return logicalResourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getRegion() {
        return region;
    }

    public String getExecutionRoleArn() {
        return executionRoleArn;
    }

    public String getStackId() {
        return stackId;
    }

    public String getSfnResponseToken() {
        return sfnResponseToken;
    }

    public String getStepName() {
        return stepName;
    }

    public String getOutputPath() {
        return outputPath;
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

    public String getClientRequestToken() {
        return clientRequestToken;
    }

    public String getDesiredState() {
        return desiredState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StepGuardRequest that = (StepGuardRequest) o;
        return Objects.equals(stackName, that.stackName) && Objects.equals(resourceStatus,
                                                                           that.resourceStatus) && Objects.equals(
                logicalResourceId,
                that.logicalResourceId) && Objects.equals(resourceType,
                                                          that.resourceType) && Objects.equals(region,
                                                                                               that.region) && Objects.equals(
                executionRoleArn,
                that.executionRoleArn) && Objects.equals(stackId, that.stackId) && Objects.equals(
                sfnResponseToken,
                that.sfnResponseToken) && Objects.equals(
                stepName,
                that.stepName) && Objects.equals(outputPath, that.outputPath) && Objects.equals(
                environment,
                that.environment) && Objects.equals(distributionId,
                                                    that.distributionId) && Objects.equals(
                distributionName,
                that.distributionName) && Objects.equals(objectIdentifier,
                                                         that.objectIdentifier) && Objects.equals(
                clientRequestToken,
                that.clientRequestToken) && Objects.equals(desiredState, that.desiredState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackName,
                            resourceStatus,
                            logicalResourceId,
                            resourceType,
                            region,
                            executionRoleArn,
                            stackId,
                            sfnResponseToken,
                            stepName,
                            outputPath,
                            environment,
                            distributionId,
                            distributionName,
                            objectIdentifier,
                            clientRequestToken,
                            desiredState);
    }

    @Override
    public String toString() {
        return "StepGuardRequest{" +
               "stackName='" + stackName + '\'' +
               ", resourceStatus='" + resourceStatus + '\'' +
               ", logicalResourceId='" + logicalResourceId + '\'' +
               ", resourceType='" + resourceType + '\'' +
               ", region='" + region + '\'' +
               ", executionRoleArn='" + executionRoleArn + '\'' +
               ", stackId='" + stackId + '\'' +
               ", sfnResponseToken='" + sfnResponseToken + '\'' +
               ", stepName='" + stepName + '\'' +
               ", outputPath='" + outputPath + '\'' +
               ", environment='" + environment + '\'' +
               ", distributionId='" + distributionId + '\'' +
               ", distributionName='" + distributionName + '\'' +
               ", objectIdentifier='" + objectIdentifier + '\'' +
               ", clientRequestToken='" + clientRequestToken + '\'' +
               ", desiredState='" + desiredState + '\'' +
               '}';
    }

    public static class Builder {
        private String stackName;
        private String resourceStatus;
        private String logicalResourceId;
        private String resourceType;
        private String region;
        private String executionRoleArn;
        private String stackId;
        private String sfnResponseToken;
        private String stepName;
        private String outputPath;
        private Environment environment;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private ObjectIdentifier objectIdentifier;
        private String clientRequestToken;
        private DesiredState desiredState;


        private Builder() {
        }

        public Builder setStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setResourceStatus(String resourceStatus) {
            this.resourceStatus = resourceStatus;
            return this;
        }

        public Builder setLogicalResourceId(String logicalResourceId) {
            this.logicalResourceId = logicalResourceId;
            return this;
        }

        public Builder setResourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }
        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }
        public Builder setExecutionRoleArn(String executionRoleArn) {
            this.executionRoleArn = executionRoleArn;
            return this;
        }

        public Builder setStackId(String stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder setSfnResponseToken(String sfnResponseToken) {
            this.sfnResponseToken = sfnResponseToken;
            return this;
        }

        public Builder setStepName(String stepName){
            this.stepName = stepName;
            return this;
        }

        public Builder setOutputPath(String outputPath){
            this.outputPath = outputPath;
            return this;
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDistributionId(DistributionId distributionId){
            this.distributionId = distributionId;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName){
            this.distributionName = distributionName;
            return this;
        }

        public Builder setObjectIdentifier(ObjectIdentifier objectIdentifier){
            this.objectIdentifier = objectIdentifier;
            return this;
        }

        public Builder setClientRequestToken(String clientRequestToken){
            this.clientRequestToken = clientRequestToken;
            return this;
        }

        public Builder setDesiredState(DesiredState desiredState){
            this.desiredState = desiredState;
            return this;
        }


        public StepGuardRequest build() {
            return new StepGuardRequest(this);
        }
    }
}
