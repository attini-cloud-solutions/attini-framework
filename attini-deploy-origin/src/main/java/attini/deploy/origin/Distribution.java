package attini.deploy.origin;


import static java.util.Objects.requireNonNull;

import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Version;

public class Distribution {

    private final DistributionName distributionName;
    private final DistributionId distributionId;

    private final String outputUrl;

    private final Version version;

    public Distribution(DistributionName distributionName, DistributionId distributionId, String outputUrl, Version version) {
        this.distributionName = requireNonNull(distributionName, "distributionName");
        this.distributionId = requireNonNull(distributionId, "distributionId");
        this.outputUrl  = outputUrl;
        this.version = version;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public Optional<String> getOutputUrl() {
        return Optional.ofNullable(outputUrl);
    }


    public Optional<Version> getVersion() {
        return Optional.ofNullable(version);
    }

    @Override
    public String toString() {
        return "Distribution{" +
               "distributionName=" + distributionName +
               ", distributionId=" + distributionId +
               ", outputUrl='" + outputUrl + '\'' +
               ", version=" + version +
               '}';
    }
}
