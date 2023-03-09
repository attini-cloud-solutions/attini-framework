/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.stackdata;

import java.util.Objects;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class StackData implements ResourceState {
    private final String sfnToken;
    private final String stackId;
    private final String stepName;
    private final String outputPath;
    private final Environment environment;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final ObjectIdentifier objectIdentifier;
    private final DesiredState desiredState;

    private StackData(Builder builder) {
        this.sfnToken = builder.sfnToken;
        this.stackId = builder.stackId;
        this.stepName = builder.stepName;
        this.outputPath = builder.outputPath;
        this.environment = builder.environment;
        this.distributionId = builder.distributionId;
        this.distributionName = builder.distributionName;
        this.objectIdentifier = builder.objectIdentifier;
        this.desiredState = builder.desiredState;
    }

    public static Builder builder() {
        return new Builder();
    }



    public Optional<String> getSfnToken() {
        return Optional.ofNullable(sfnToken);
    }

    public Optional<String> getStackId() {
        return Optional.ofNullable(stackId);
    }

    public Optional<String> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public String getStepName() {
        return stepName;
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

    public DesiredState getDesiredState() {
        return desiredState;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackData stackData = (StackData) o;
        return Objects.equals(sfnToken, stackData.sfnToken) && Objects.equals(stackId,
                                                                              stackData.stackId) && Objects.equals(
                stepName,
                stackData.stepName) && Objects.equals(outputPath,
                                                      stackData.outputPath) && Objects.equals(
                environment,
                stackData.environment) && Objects.equals(distributionId,
                                                         stackData.distributionId) && Objects.equals(
                distributionName,
                stackData.distributionName) && Objects.equals(objectIdentifier,
                                                              stackData.objectIdentifier) && desiredState == stackData.desiredState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sfnToken,
                            stackId,
                            stepName,
                            outputPath,
                            environment,
                            distributionId,
                            distributionName,
                            objectIdentifier,
                            desiredState);
    }

    @Override
    public String toString() {
        return "StackData{" +
               "sfnToken='" + sfnToken + '\'' +
               ", stackId='" + stackId + '\'' +
               ", stepName='" + stepName + '\'' +
               ", outputPath='" + outputPath + '\'' +
               ", environment=" + environment +
               ", distributionId=" + distributionId +
               ", distributionName=" + distributionName +
               ", objectIdentifier=" + objectIdentifier +
               ", desiredState=" + desiredState +
               '}';
    }

    public static class Builder {
        private String sfnToken;
        private String stackId;
        private String stepName;
        private String outputPath;
        private Environment environment;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private ObjectIdentifier objectIdentifier;
        private DesiredState desiredState;

        private Builder() {
        }

        public Builder setSfnToken(String sfnToken) {
            this.sfnToken = sfnToken;
            return this;
        }

        public Builder setStackId(String stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder setStepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
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

        public Builder setDesiredState(DesiredState desiredState){
            this.desiredState = desiredState;
            return this;
        }

        public StackData build() {
            return new StackData(this);
        }
    }
}
