package attini.action.actions.deploycloudformation.stackconfig;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;

public class GetConfigFileRequest {

    private final Environment environment;
    private final DistributionName distributionName;
    private final DistributionId distributionId;
    private final String configPath;
    private final Map<String, String> variables;

    private GetConfigFileRequest(Builder builder) {
        this.environment = requireNonNull(builder.environment, "environment");
        this.distributionName = requireNonNull(builder.distributionName, "distributionName");
        this.distributionId = requireNonNull(builder.distributionId, "distributionId");
        this.configPath = requireNonNull(builder.configPath, "configPath");
        this.variables = builder.variables;
    }


    public static Builder builder() {
        return new Builder();
    }


    public Environment getEnvironment() {
        return environment;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public String getConfigPath() {
        return configPath;
    }

    public Map<String, String> getVariables() {
       if (variables == null){
           return Collections.emptyMap();
       }
       return variables;
    }




    @Override
    public String toString() {
        return "GetConfigFileRequest{" +
               "environment=" + environment +
               ", distributionName=" + distributionName +
               ", distributionId=" + distributionId +
               ", configPath='" + configPath + '\'' +
               ", variables=" + variables +
               '}';
    }

    public static class Builder {
        private Environment environment;
        private DistributionName distributionName;
        private DistributionId distributionId;
        private String configPath;
        private Map<String, String> variables;

        private Builder() {
        }

        public Builder setEnvironment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder setDistributionName(DistributionName distributionName) {
            this.distributionName = distributionName;
            return this;
        }

        public Builder setDistributionId(DistributionId distributionId) {
            this.distributionId = distributionId;
            return this;
        }

        public Builder setConfigPath(String configPath) {
            this.configPath = configPath;
            return this;
        }

        public Builder setVariables(Map<String, String> variables) {
            this.variables = variables;
            return this;
        }

        public Builder of(GetConfigFileRequest getConfigFileRequest) {
            this.environment = getConfigFileRequest.environment;
            this.distributionName = getConfigFileRequest.distributionName;
            this.distributionId = getConfigFileRequest.distributionId;
            this.configPath = getConfigFileRequest.configPath;
            this.variables = getConfigFileRequest.variables;
            return this;
        }

        public GetConfigFileRequest build() {
            return new GetConfigFileRequest(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetConfigFileRequest that = (GetConfigFileRequest) o;
        return Objects.equals(environment, that.environment) && Objects.equals(distributionName,
                                                                               that.distributionName) && Objects.equals(
                distributionId,
                that.distributionId) && Objects.equals(configPath, that.configPath) && Objects.equals(
                variables,
                that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environment, distributionName, distributionId, configPath, variables);
    }
}
