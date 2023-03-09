/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import attini.action.domain.DesiredState;
import attini.action.domain.OnFailure;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Tag;

public class StackConfiguration {

    private final Collection<Tag> tags;
    private final Collection<Parameter> parameters;
    private final String stackName;
    private final String template;
    private final String region;
    private final String executionRole;
    private final String stackRole;
    private final String outputPath;
    private final DesiredState desiredState;
    private final Boolean enableTerminationProtection;
    private final OnFailure onFailure;

    private StackConfiguration(Builder builder) {
        this.tags = builder.tags;
        this.parameters = builder.parameters;
        this.stackName = requireNonNull(builder.stackName, "tackName");
        this.template = requireNonNull(builder.template, "template");
        this.region = builder.region;
        this.executionRole = builder.executionRole;
        this.stackRole = builder.stackRole;
        this.outputPath = builder.outputPath;
        this.desiredState = requireNonNull(builder.desiredState, "desiredState");
        this.enableTerminationProtection = builder.enableTerminationProtection;
        this.onFailure = builder.onFailure;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Collection<Tag> getTags() {
        return tags == null ? Collections.emptyList() : tags;
    }

    public Collection<Parameter> getParameters() {
        return parameters == null ? Collections.emptyList() : parameters;
    }

    public String getStackName() {
        return stackName;
    }

    public String getTemplate() {
        return template;
    }

    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }

    public Optional<String> getExecutionRole() {
        return Optional.ofNullable(executionRole);
    }

    public Optional<String> getStackRole() {
        return Optional.ofNullable(stackRole);
    }

    public Optional<String> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public DesiredState getDesiredState() {
        return desiredState;
    }

    public Optional<Boolean> getEnableTerminationProtection() {
        return Optional.ofNullable(enableTerminationProtection);
    }

    public Optional<OnFailure> getOnFailure() {
        return Optional.ofNullable(onFailure);
    }

    public static class Builder {
        private String stackRole;
        private Collection<Tag> tags;
        private Collection<Parameter> parameters;
        private String stackName;
        private String template;
        private String region;
        private String executionRole;
        private String outputPath;
        private DesiredState desiredState;
        private Boolean enableTerminationProtection;
        private OnFailure onFailure;

        private Builder() {
        }

        public Builder setTags(Collection<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public Builder setParameters(Collection<Parameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder setStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setTemplate(String template) {
            this.template = template;
            return this;
        }

        public Builder setRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder setExecutionRole(String executionRole) {
            this.executionRole = executionRole;
            return this;
        }

        public Builder setStackRole(String stackRole) {
            this.stackRole = stackRole;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder setDesiredState(DesiredState desiredState) {
            this.desiredState = desiredState;
            return this;
        }

        public Builder setOnFailure(OnFailure onFailure) {
            this.onFailure = onFailure;
            return this;
        }
        public Builder setEnableTerminationProtection(Boolean enableTerminationProtection) {
            this.enableTerminationProtection = enableTerminationProtection;
            return this;
        }



        public StackConfiguration build() {
            return new StackConfiguration(this);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackConfiguration that = (StackConfiguration) o;
        return Objects.equals(tags, that.tags) && Objects.equals(parameters,
                                                                 that.parameters) && Objects.equals(
                stackName,
                that.stackName) && Objects.equals(template, that.template) && Objects.equals(region,
                                                                                             that.region) && Objects.equals(
                executionRole,
                that.executionRole) && Objects.equals(stackRole, that.stackRole) && Objects.equals(
                outputPath,
                that.outputPath) && desiredState == that.desiredState && Objects.equals(
                enableTerminationProtection,
                that.enableTerminationProtection) && Objects.equals(onFailure, that.onFailure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags,
                            parameters,
                            stackName,
                            template,
                            region,
                            executionRole,
                            stackRole,
                            outputPath,
                            desiredState,
                            enableTerminationProtection,
                            onFailure);
    }

    @Override
    public String toString() {
        return "StackConfiguration{" +
               "tags=" + tags +
               ", parameters=" + parameters +
               ", stackName='" + stackName + '\'' +
               ", template='" + template + '\'' +
               ", region='" + region + '\'' +
               ", executionRole='" + executionRole + '\'' +
               ", stackRole='" + stackRole + '\'' +
               ", outputPath='" + outputPath + '\'' +
               ", desiredState=" + desiredState +
               ", enableTerminationProtection='" + enableTerminationProtection + '\'' +
               ", onFailure='" + onFailure + '\'' +
               '}';
    }
}
