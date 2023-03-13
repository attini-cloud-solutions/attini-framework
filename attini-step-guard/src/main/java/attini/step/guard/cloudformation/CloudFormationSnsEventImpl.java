/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.cloudformation;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

public class CloudFormationSnsEventImpl implements CloudFormationSnsEvent {
    private final String stackName;
    private final String resourceStatus;
    private final String logicalResourceId;
    private final String clientRequestToken;
    private final String resourceType;
    private final String stackId;
    private final String resourceStatusReason;


    private CloudFormationSnsEventImpl(Builder builder) {
        this.stackName = requireNonNull(builder.stackName, "stackName");
        this.resourceStatus = requireNonNull(builder.resourceStatus, "resourceStatus");
        this.logicalResourceId = requireNonNull(builder.logicalResourceId, "logicalResourceId");
        this.clientRequestToken = requireNonNull(builder.clientRequestToken, "clientRequestToken");
        this.resourceType = requireNonNull(builder.resourceType, "resourceType");
        this.stackId = builder.stackId;
        this.resourceStatusReason = builder.resourceStatusReason;
    }

    public Optional<String> getResourceStatusReason() {
        return Optional.ofNullable(resourceStatusReason);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getStackName() {
        return stackName;
    }

    @Override
    public String getResourceStatus() {
        return resourceStatus;
    }

    public String getLogicalResourceId() {
        return logicalResourceId;
    }

    @Override
    public Optional<String> getRegion() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getStackId() {
        return Optional.ofNullable(stackId);
    }

    @Override
    public Optional<String> getExecutionRoleArn() {
        return Optional.empty();
    }

    @Override
    public String getClientRequestToken() {
        return clientRequestToken;
    }

    public String getResourceType() {
        return resourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CloudFormationSnsEventImpl that = (CloudFormationSnsEventImpl) o;
        return Objects.equals(stackName, that.stackName) && Objects.equals(resourceStatus,
                                                                           that.resourceStatus) && Objects.equals(
                logicalResourceId,
                that.logicalResourceId) && Objects.equals(clientRequestToken,
                                                          that.clientRequestToken) && Objects.equals(
                resourceType,
                that.resourceType) && Objects.equals(stackId, that.stackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackName, resourceStatus, logicalResourceId, clientRequestToken, resourceType, stackId);
    }

    @Override
    public String toString() {
        return "CloudFormationSnsEvent{" +
               "stackName='" + stackName + '\'' +
               ", resourceStatus='" + resourceStatus + '\'' +
               ", logicalResourceId='" + logicalResourceId + '\'' +
               ", clientRequestToken='" + clientRequestToken + '\'' +
               ", resourceType='" + resourceType + '\'' +
               ", stackId='" + stackId + '\'' +
               '}';
    }

    public static class Builder {
        private String stackName;
        private String resourceStatus;
        private String logicalResourceId;
        private String clientRequestToken;
        private String resourceType;
        private String stackId;
        private String resourceStatusReason;

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

        public Builder setClientRequestToken(String clientRequestToken) {
            this.clientRequestToken = clientRequestToken;
            return this;
        }

        public Builder setResourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder setStackId(String stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder setResourceStatusReason(String resourceStatusReason) {
            this.resourceStatusReason = resourceStatusReason;
            return this;
        }

        public CloudFormationSnsEventImpl build() {
            return new CloudFormationSnsEventImpl(this);
        }
    }
}
