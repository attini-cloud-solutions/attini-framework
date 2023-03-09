package attini.deploy.origin.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Version;

public interface AttiniConfig {

    DistributionId getAttiniDistributionId();

    DistributionName getAttiniDistributionName();

    Map<String, String> getAttiniDistributionTags();

    Optional<InitDeployStackConfig> getAttiniInitDeployStackConfig();

    List<DistributionDependency> getDependencies();

    Optional<Version> getVersion();
}
