package attini.action.facades.stackdata;

import java.util.Map;

import attini.domain.Environment;

public interface AppDeploymentPlanDataFacade {

    Map<String, String> getStackParameters(String appDeploymentPlan, Environment environment);
}
