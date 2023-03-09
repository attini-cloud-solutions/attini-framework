/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import attini.step.guard.stackdata.DesiredState;

public class CloudFormationManualTriggerEvent implements CloudFormationEvent {
    private final String stackName;
    private final String resourceStatus;
    private final String region;
    private final String executionRoleArn;
    private final String stackId;
    private final String sfnResponseToken;
    private final String stepName;
    private final String outputPath;
    private final AttiniContext attiniContext;
    private final String clientRequestToken;
    private final DesiredState desiredState;
    private final String logicalResourceId;

    private CloudFormationManualTriggerEvent(Builder builder) {
        this.stackName = requireNonNull(builder.stackName, "stackName");
        this.resourceStatus = requireNonNull(builder.resourceStatus, "resourceStatus");
        this.region = builder.region;
        this.executionRoleArn = builder.executionRoleArn;
        this.stackId = builder.stackId;
        this.sfnResponseToken = builder.sfnResponseToken;
        this.stepName = requireNonNull(builder.stepName, "stepName");
        this.outputPath = builder.outputPath;
        this.attiniContext = requireNonNull(builder.attiniContext, "attiniContext");
        this.clientRequestToken = requireNonNull(builder.clientRequestToken, "clientRequestToken");
        this.desiredState = requireNonNull(builder.desiredState, "desiredState");
        this.logicalResourceId = requireNonNull(builder.logicalResourceId, "logicalResourceId");

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

    @Override
    public String getLogicalResourceId() {
        return logicalResourceId;
    }

    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }

    public Optional<String> getStackId() {
        return Optional.ofNullable(stackId);
    }

    public Optional<String> getExecutionRoleArn() {
        return Optional.ofNullable(executionRoleArn);
    }

    public Optional<String> getSfnResponseToken() {
        return Optional.ofNullable(sfnResponseToken);
    }

    public String getStepName(){
        return stepName;
    }

    public Optional<String> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public AttiniContext getAttiniContext(){
        return attiniContext;
    }

    public String getClientRequestToken() {
        return clientRequestToken;
    }

    public DesiredState getDesiredState(){
        return desiredState;
    }

    @Override
    public String toString() {
        return "StepGuardInput{" +
               "stackName='" + stackName + '\'' +
               ", resourceStatus='" + resourceStatus + '\'' +
               ", region='" + region + '\'' +
               ", executionRoleArn='" + executionRoleArn + '\'' +
               ", stackId='" + stackId + '\'' +
               ", sfnResponseToken='" + sfnResponseToken + '\'' +
               ", stepName='" + stepName + '\'' +
               ", outputPath='" + outputPath + '\'' +
               '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudFormationManualTriggerEvent that = (CloudFormationManualTriggerEvent) o;
        return Objects.equals(stackName, that.stackName) && Objects.equals(resourceStatus,
                                                                           that.resourceStatus) && Objects.equals(
                region,
                that.region) && Objects.equals(executionRoleArn,
                                               that.executionRoleArn) && Objects.equals(stackId,
                                                                                        that.stackId) && Objects.equals(
                sfnResponseToken,
                that.sfnResponseToken) && Objects.equals(
                stepName,
                that.stepName) && Objects.equals(outputPath, that.outputPath) && Objects.equals(
                attiniContext,
                that.attiniContext) && Objects.equals(clientRequestToken,
                                                      that.clientRequestToken) && desiredState == that.desiredState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackName,
                            resourceStatus,
                            region,
                            executionRoleArn,
                            stackId,
                            sfnResponseToken,
                            stepName,
                            outputPath,
                            attiniContext,
                            clientRequestToken,
                            desiredState);
    }

    public static class Builder {
        private String stackName;
        private String resourceStatus;
        private String region;
        private String executionRoleArn;
        private String stackId;
        private String sfnResponseToken;
        private String stepName;
        private String outputPath;
        private AttiniContext attiniContext;
        private String clientRequestToken;
        private DesiredState desiredState;
        private String logicalResourceId;


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

        public Builder setStepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder setAttiniContext(AttiniContext attiniContext) {
            this.attiniContext = attiniContext;
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

        public Builder setLogicalResourceId(String logicalResourceId){
            this.logicalResourceId = logicalResourceId;
            return this;
        }


        public CloudFormationManualTriggerEvent build() {
            return new CloudFormationManualTriggerEvent(this);
        }
    }
}
