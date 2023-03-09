package attini.action.domain;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;


@Builder
@EqualsAndHashCode
@ToString
public class Distribution {
    @NonNull
    private final DistributionId distributionId;
    @NonNull
    private final DistributionName distributionName;
    @NonNull
    private final Environment environment;
    @NonNull
    private final String deploymentSourcePrefix;
    private final String outputUrl;
    private final List<DistributionDependency> dependencies;



    public DistributionId getDistributionId() {
        return distributionId;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public List<DistributionDependency> getDependencies() {
        return dependencies == null ? Collections.emptyList() : dependencies;
    }

    public String getDeploymentSourcePrefix() {
        return deploymentSourcePrefix;
    }

    public Optional<String> getOutputUrl() {
        return Optional.ofNullable(outputUrl);
    }
}
