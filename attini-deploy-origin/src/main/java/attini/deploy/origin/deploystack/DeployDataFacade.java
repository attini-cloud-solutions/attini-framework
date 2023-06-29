package attini.deploy.origin.deploystack;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import attini.deploy.origin.DistributionData;
import attini.domain.DistributionContext;
import attini.domain.DistributionName;
import attini.domain.Environment;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

public interface DeployDataFacade {

    void savePlatformDeployment(SaveDeploymentDataRequest request);

    void saveAppDeployment(SaveDeploymentDataRequest request,
              String stackName, String sfnArn);

    int countDeployDataAfterDate(DistributionName distributionName, Environment environment, LocalDate from);

    DeployData getLatestDeployData(DistributionName distributionName, Environment environment);

    List<DeployData> getDeployData(DistributionName distributionName, Environment environment, LocalDate to);

    @Builder
    @EqualsAndHashCode
    @ToString
    class SaveDeploymentDataRequest{
        private DistributionData distributionData;
        @NonNull
        private DistributionContext distributionContext;
        private InitDeployError error;
        private long deployTime;
        private boolean isUnchanged;

        public Optional<DistributionData> getDistributionData() {
            return Optional.ofNullable(distributionData);
        }

        public DistributionContext getDistributionContext() {
            return distributionContext;
        }

        public Optional<InitDeployError> getError() {
            return Optional.ofNullable(error);
        }

        public long getDeployTime() {
            return deployTime;
        }

        public boolean isUnchanged() {
            return isUnchanged;
        }
    }

}
