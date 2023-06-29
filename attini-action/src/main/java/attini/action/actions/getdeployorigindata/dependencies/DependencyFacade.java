package attini.action.actions.getdeployorigindata.dependencies;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.Map;

import attini.action.facades.stackdata.DistributionDataFacade;
import attini.domain.DistributionName;
import attini.domain.Environment;

public class DependencyFacade {

    private final DistributionDataFacade distributionDataFacade;

    public DependencyFacade(DistributionDataFacade distributionDataFacade) {
        this.distributionDataFacade = requireNonNull(distributionDataFacade, "distributionDataFacade");
    }

    public Map<String, Dependency> getDependencies(Environment environment, DistributionName distributionName) {

        return distributionDataFacade.getDistribution(distributionName, environment)
                                     .getDependencies()
                                     .stream()
                                     .map(dependency -> distributionDataFacade.getDistribution(DistributionName.of(
                                             dependency.name()), environment))
                                     .collect(toMap(distribution -> distribution.getDistributionName()
                                                                                .asString(),
                                                    distribution -> new Dependency(distribution.getDeploymentSourcePrefix(),
                                                                                   distribution.getOutputUrl()
                                                                                               .orElse(null))));
    }
}
