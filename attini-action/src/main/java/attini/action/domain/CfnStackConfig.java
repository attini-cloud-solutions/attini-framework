package attini.action.domain;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import lombok.Builder;

@Builder(setterPrefix = "set", builderClassName = "Builder", toBuilder = true)
public class CfnStackConfig {
    private final String stackName;
    private final String templateUrlPrefix;
    private final String template;
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

    public Optional<String> getStackName() {
        return Optional.ofNullable(stackName);
    }

    public Optional<String> getTemplate() {
        return Optional.ofNullable(template);
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


    @Override
    public String toString() {
        return "CfnStackConfig{" +
               "stackName='" + stackName + '\'' +
               ", templateUrlPrefix='" + templateUrlPrefix + '\'' +
               ", templateUrl='" + template + '\'' +
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
