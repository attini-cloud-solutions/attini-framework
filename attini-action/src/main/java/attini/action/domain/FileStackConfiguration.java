/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.domain;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class FileStackConfiguration {

    private final String stackName;
    private final String templatePath;
    private final String region;
    private final String executionRole;
    private final String stackRoleArn;
    private final Map<String, ConfigurationPropertyValue> tags;
    private final Map<String, ConfigurationPropertyValue> parameters;
    private final String outputPath;
    private final String action;
    private final String enableTerminationProtection;
    private final String onFailure;

    private FileStackConfiguration(Builder builder) {
        this.stackName = builder.stackName;
        this.templatePath = builder.templatePath;
        this.tags = builder.tags;
        this.parameters = builder.parameters;
        this.region = builder.region;
        this.executionRole = builder.executionRole;
        this.stackRoleArn = builder.stackRoleArn;
        this.outputPath = builder.outputPath;
        this.action = builder.action;
        this.enableTerminationProtection = builder.enableTerminationProtection;
        this.onFailure = builder.onFailure;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Map<String, ConfigurationPropertyValue> getTags() {
        return tags != null ? tags : Collections.emptyMap();
    }

    public Map<String, ConfigurationPropertyValue> getParameters() {
        return parameters != null ? parameters : Collections.emptyMap();
    }


    public String getStackName() {
        return stackName;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }

    public Optional<String> getExecutionRole() {
        return Optional.ofNullable(executionRole);
    }

    public Optional<String> getStackRoleArn() {
        return Optional.ofNullable(stackRoleArn);
    }

    public Optional<String> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public Optional<String> getAction(){
        return Optional.ofNullable(action);
    }

    public Optional<String> getEnableTerminationProtection() {
        return Optional.ofNullable(enableTerminationProtection);
    }

    public Optional<String> getOnFailure() {
        return Optional.ofNullable(onFailure);
    }

    public static class Builder {
        public String stackRoleArn;
        private String stackName;
        private String templatePath;
        private Map<String, ConfigurationPropertyValue> tags;
        private Map<String, ConfigurationPropertyValue> parameters;
        private String region;
        private String executionRole;
        private String outputPath;
        private String action;
        private String enableTerminationProtection;
        private String onFailure;

        private Builder() {
        }

        public Builder setStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setTemplatePath(String templatePath) {
            this.templatePath = templatePath;
            return this;
        }

        public Builder setTags(Map<String, ConfigurationPropertyValue> tags) {
            this.tags = tags;
            return this;
        }

        public Builder setParameters(Map<String, ConfigurationPropertyValue> parameters) {
            this.parameters = parameters;
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

        public Builder setStackRoleArn(String stackRoleArn) {
            this.stackRoleArn = stackRoleArn;
            return this;
        }

        public Builder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        public Builder setOnFailure(String onFailure) {
            this.onFailure = onFailure;
            return this;
        }
        public Builder setEnableTerminationProtection(String enableTerminationProtection) {
            this.enableTerminationProtection = enableTerminationProtection;
            return this;
        }


        public FileStackConfiguration build() {
            return new FileStackConfiguration(this);
        }
    }


    @Override
    public String toString() {
        return "FileStackConfiguration{" +
               "stackName='" + stackName + '\'' +
               ", templatePath='" + templatePath + '\'' +
               ", region='" + region + '\'' +
               ", executionRole='" + executionRole + '\'' +
               ", stackRoleArn='" + stackRoleArn + '\'' +
               ", tags=" + tags +
               ", parameters=" + parameters +
               ", outputPath='" + outputPath + '\'' +
               ", action='" + action + '\'' +
               ", enableTerminationProtection='" + enableTerminationProtection + '\'' +
               ", onFailure='" + onFailure + '\'' +
               '}';
    }
}
