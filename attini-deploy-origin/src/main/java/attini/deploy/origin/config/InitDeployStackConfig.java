package attini.deploy.origin.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import attini.domain.Environment;

public class InitDeployStackConfig {
    private static final Environment DEFAULT_CONFIG_KEY = Environment.of("default");
    private final String attiniInitDeployTemplatePath;
    private final String initDeployStackName;
    private final Map<String, String> parameters;
    private final HashMap<Environment, HashMap<String, String>> tags;
    private final HashMap<Environment, HashMap<String, String>> variables;
    private final boolean forceUpdate;



    private InitDeployStackConfig(Builder builder) {
        this.attiniInitDeployTemplatePath = builder.attiniInitDeployTemplatePath == null ? "ci-cd/attini-init-deploy.yaml" : builder.attiniInitDeployTemplatePath;
        this.initDeployStackName = builder.attiniInitDeployStackName;
        this.parameters = builder.parameters;
        this.tags = builder.tags;
        this.variables = builder.variables;
        this.forceUpdate = builder.forceUpdate;
    }

    public String getAttiniInitDeployTemplatePath() {
        return attiniInitDeployTemplatePath;
    }

    public String getInitDeployStackName() {
        return initDeployStackName;
    }

    public Map<String, String> getParameters() {
        return parameters != null ? parameters : Collections.emptyMap();
    }

    public Map<String, String> getTags(Environment environment) {
        return getEnvironmentConfig(tags, environment);
    }

    public Map<String, String> getVariables(Environment environment) {
        return getEnvironmentConfig(variables, environment);
    }

    private static Map<String, String> getEnvironmentConfig(HashMap<Environment, HashMap<String, String>> config,
                                                            Environment environmentName) {
        Map<String, String> finalConfig = new HashMap<>();
        if (config != null) {
            if (config.get(DEFAULT_CONFIG_KEY) != null) {
                finalConfig.putAll(config.get(DEFAULT_CONFIG_KEY));
            }
            if (config.get(environmentName) != null) {
                finalConfig.putAll(config.get(environmentName));
            }
        }
        return finalConfig;
    }

    public boolean forceUpdate() {
        return forceUpdate;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitDeployStackConfig that = (InitDeployStackConfig) o;
        return forceUpdate == that.forceUpdate && Objects.equals(attiniInitDeployTemplatePath,
                                                                 that.attiniInitDeployTemplatePath) && Objects.equals(
                initDeployStackName,
                that.initDeployStackName) && Objects.equals(parameters,
                                                            that.parameters) && Objects.equals(
                tags,
                that.tags) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attiniInitDeployTemplatePath,
                            initDeployStackName,
                            parameters,
                            tags,
                            variables,
                            forceUpdate);
    }

    @Override
    public String toString() {
        return "AttiniInitDeployStackConfig{" +
               "attiniInitDeployTemplatePath='" + attiniInitDeployTemplatePath + '\'' +
               ", attiniInitDeployStackName='" + initDeployStackName + '\'' +
               ", parameters=" + parameters +
               ", tags=" + tags +
               ", variables=" + variables +
               ", forceUpdate=" + forceUpdate +
               '}';
    }

    public static final class Builder {
        private String attiniInitDeployTemplatePath;
        private String attiniInitDeployStackName;
        private Map<String, String> parameters;
        private HashMap<Environment, HashMap<String, String>> tags;
        private HashMap<Environment, HashMap<String, String>> variables;
        private boolean forceUpdate;

        public Builder() {
        }

        public Builder attiniInitDeployTemplatePath(String val) {
            attiniInitDeployTemplatePath = val;
            return this;
        }

        public Builder attiniInitDeployStackName(String val) {
            attiniInitDeployStackName = val;
            return this;
        }

        public Builder parameters(Map<String, String> val) {
            parameters = val;
            return this;
        }

        public Builder tags(HashMap<Environment, HashMap<String, String>> val) {
            tags = val;
            return this;
        }

        public Builder variables(HashMap<Environment, HashMap<String, String>> val) {
            variables = val;
            return this;
        }

        public Builder forceUpdate(boolean val){
            forceUpdate = val;
            return this;
        }

        public InitDeployStackConfig build() {
            return new InitDeployStackConfig(this);
        }
    }
}
