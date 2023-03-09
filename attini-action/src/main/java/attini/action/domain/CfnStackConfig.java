package attini.action.domain;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class CfnStackConfig {
    private final String stackName;
    private final String templateUrlPrefix;
    private final String templateUrl;
    private final String stackRole;
    private final String configPath;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final Environment environment;
    private final Map<String, ConfigurationPropertyValue> parameters;
    private final Map<String, ConfigurationPropertyValue> tags;
    private final ObjectIdentifier objectIdentifier;
    private final Map<String, String> variables;
    private final String region;
    private final String executionRole;
    private final String outputPath;
    private final String action;
    private final String initStackName;
    private final String enableTerminationProtection;
    private final String onFailure;


    private CfnStackConfig(Builder builder) {
        this.stackName = builder.stackName;
        this.templateUrl = builder.template;
        this.stackRole = builder.stackRole;
        this.configPath = builder.configPath;
        this.parameters = builder.parameters;
        this.distributionId = requireNonNull(builder.distributionId, " distributionId");
        this.distributionName = requireNonNull(builder.distributionName, "distributionName");
        this.environment = requireNonNull(builder.environment, "environment");
        this.tags = builder.tags;
        this.objectIdentifier = builder.objectIdentifier;
        this.templateUrlPrefix = builder.templateUrlPrefix;
        this.variables = builder.variables;
        this.region = builder.region;
        this.executionRole = builder.executionRole;
        this.outputPath = builder.outputPath;
        this.action =builder.action;
        this.initStackName = requireNonNull(builder.initStackName, "initStackName");
        this.enableTerminationProtection = builder.enableTerminationProtection;
        this.onFailure = builder.onFailure;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Optional<String> getStackName() {
        return Optional.ofNullable(stackName);
    }

    public Optional<String> getTemplateUrl() {
        return Optional.ofNullable(templateUrl);
    }

    public String getTemplateUrlPrefix() {
        return templateUrlPrefix;
    }

    public Optional<String> getStackRole() {
        return Optional.ofNullable(stackRole);
    }

    public Optional<String> getConfigPath() {
        return Optional.ofNullable(configPath);
    }

    public Map<String, ConfigurationPropertyValue> getParameters() {
        return parameters != null ? parameters : emptyMap();
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
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

    public Map<String, String> getVariables() {
        return Optional.ofNullable(variables).orElse(new HashMap<>());
    }

    public Map<String, ConfigurationPropertyValue> getTags() {
        return tags != null ? tags : emptyMap();
    }


    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }

    public Optional<String> getExecutionRole() {
        return Optional.ofNullable(executionRole);
    }

    public Optional<String> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public Optional<String> getAction() {
        return Optional.ofNullable(action);
    }

    public String getInitStackName() {
        return initStackName;
    }

    public Optional<String> getEnableTerminationProtection() {
        return Optional.ofNullable(enableTerminationProtection);
    }

    public Optional<String> getOnFailure() {
        return Optional.ofNullable(onFailure);
    }

    public static class Builder {
        private String stackName;
        private String template;
        private String stackRole;
        private String configPath;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private Environment environment;
        private Map<String, ConfigurationPropertyValue> parameters;
        private Map<String, ConfigurationPropertyValue> tags;
        private ObjectIdentifier objectIdentifier;
        private String templateUrlPrefix;
        private Map<String, String> variables;
        private String region;
        private String executionRole;
        private String outputPath;
        private String action;
        private String initStackName;
        private String enableTerminationProtection;
        private String onFailure;


        private Builder() {
        }

        public Builder setStackName(String stackName) {
            this.stackName = stackName;
            return this;
        }

        public Builder setTemplate(String template) {
            this.template = template;
            return this;
        }

        public Builder setStackRole(String stackRole) {
            this.stackRole = stackRole;
            return this;
        }

        public Builder setConfigPath(String configPath) {
            this.configPath = configPath;
            return this;
        }

        public Builder setParameters(Map<String, ConfigurationPropertyValue> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder setTags(Map<String, ConfigurationPropertyValue> tags) {
            this.tags = tags;
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

        public Builder setTemplateUrlPrefix(String templateUrlPrefix) {
            this.templateUrlPrefix = templateUrlPrefix;
            return this;
        }

        public Builder setVariables(Map<String, String> variables) {
            this.variables = variables;
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

        public Builder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        public Builder setInitStackName(String initStackName) {
            this.initStackName = initStackName;
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

        public CfnStackConfig build() {
            return new CfnStackConfig(this);
        }
    }


    @Override
    public String toString() {
        return "CfnStackConfig{" +
               "stackName='" + stackName + '\'' +
               ", templateUrlPrefix='" + templateUrlPrefix + '\'' +
               ", templateUrl='" + templateUrl + '\'' +
               ", stackRole='" + stackRole + '\'' +
               ", configPath='" + configPath + '\'' +
               ", distributionId=" + distributionId +
               ", distributionName=" + distributionName +
               ", environment=" + environment +
               ", parameters=" + parameters +
               ", tags=" + tags +
               ", objectIdentifier='" + objectIdentifier + '\'' +
               ", variables=" + variables +
               ", region='" + region + '\'' +
               ", executionRole='" + executionRole + '\'' +
               ", outputPath='" + outputPath + '\'' +
               ", action='" + action + '\'' +
               ", initStackName='" + initStackName + '\'' +
               ", enableTerminationProtection='" + enableTerminationProtection + '\'' +
               ", onFailure='" + onFailure + '\'' +
               '}';
    }
}
