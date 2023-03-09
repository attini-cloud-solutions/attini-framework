package attini.deploy.origin.config;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import attini.domain.DistributionName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DistributionDependency(@JsonProperty("distributionName") DistributionName distributionName,
                                     @JsonProperty("version") String version) {

    public DistributionDependency {
        requireNonNull(distributionName, "distributionName can not be null");
    }

}
