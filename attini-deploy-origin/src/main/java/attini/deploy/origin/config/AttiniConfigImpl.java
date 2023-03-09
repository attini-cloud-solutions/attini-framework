package attini.deploy.origin.config;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Version;


public class AttiniConfigImpl implements AttiniConfig {
    private final List<DistributionDependency> dependencies;
    private final InitDeployStackConfig attiniInitDeployStackConfig;
    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final Map<String, String> distributionTags;

    private final Version version;

    private AttiniConfigImpl(Builder builder) {
        dependencies = builder.dependencies;
        attiniInitDeployStackConfig = builder.attiniInitDeployStackConfig;
        distributionId = requireNonNull(builder.distributionId, "distributionId");
        distributionName = requireNonNull(builder.distributionName, "distributionName");
        distributionTags = builder.distributionTags;
        this.version = builder.version;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public DistributionId getAttiniDistributionId() {
        return distributionId;
    }

    @Override
    public DistributionName getAttiniDistributionName() {
        return distributionName;
    }

    public Map<String, String> getAttiniDistributionTags() {
        return distributionTags == null ? Collections.emptyMap() : distributionTags;
    }

    public Optional<InitDeployStackConfig> getAttiniInitDeployStackConfig() {
        return Optional.ofNullable(attiniInitDeployStackConfig);
    }

    @Override
    public List<DistributionDependency> getDependencies() {
        return dependencies == null ? Collections.emptyList() : dependencies;
    }

    @Override
    public Optional<Version> getVersion() {
        return Optional.ofNullable(version);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttiniConfigImpl that = (AttiniConfigImpl) o;
        return Objects.equals(dependencies, that.dependencies) && Objects.equals(
                attiniInitDeployStackConfig,
                that.attiniInitDeployStackConfig) && Objects.equals(distributionId,
                                                                    that.distributionId) && Objects.equals(
                distributionName,
                that.distributionName) && Objects.equals(distributionTags, that.distributionTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies,
                            attiniInitDeployStackConfig,
                            distributionId,
                            distributionName,
                            distributionTags);
    }


    @Override
    public String toString() {
        return "AttiniConfigImpl{" +
               "dependencies=" + dependencies +
               ", attiniInitDeployStackConfig=" + attiniInitDeployStackConfig +
               ", distributionId=" + distributionId +
               ", distributionName=" + distributionName +
               ", distributionTags=" + distributionTags +
               ", version=" + version +
               '}';
    }

    public static final class Builder {
        private List<DistributionDependency> dependencies;
        private InitDeployStackConfig attiniInitDeployStackConfig;
        private DistributionId distributionId;
        private DistributionName distributionName;
        private Map<String, String> distributionTags;
        private Version version;

        private Builder() {
        }

        public Builder dependencies(List<DistributionDependency> val) {
            dependencies = val;
            return this;
        }

        public Builder attiniInitDeployStackConfig(InitDeployStackConfig val) {
            attiniInitDeployStackConfig = val;
            return this;
        }

        public Builder distributionId(DistributionId val) {
            distributionId = val;
            return this;
        }

        public Builder distributionName(DistributionName val) {
            distributionName = val;
            return this;
        }

        public Builder distributionTags(Map<String, String> val) {
            distributionTags = val;
            return this;
        }

        public Builder version(Version val) {
            version = val;
            return this;
        }

        public AttiniConfigImpl build() {
            return new AttiniConfigImpl(this);
        }
    }
}
