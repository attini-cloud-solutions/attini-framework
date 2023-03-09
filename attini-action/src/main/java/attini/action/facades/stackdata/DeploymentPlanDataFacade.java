package attini.action.facades.stackdata;

import java.time.Instant;

import attini.action.domain.DeploymentPlanStateData;

public interface DeploymentPlanDataFacade {

    void saveFinalStatus(String sfnArn, String status, Instant startTime);

    DeploymentPlanStateData getDeploymentPlan(String sfnArn);
}
