package deployment.plan.transform;

import java.util.List;
import java.util.Map;

public record DeploymentPlanDefinition (Map<String, Object> definition,
                                        List<AttiniStep> attiniSteps) {
}
