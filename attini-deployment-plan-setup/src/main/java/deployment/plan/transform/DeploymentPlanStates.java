package deployment.plan.transform;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record DeploymentPlanStates(List<AttiniStep> attiniSteps,
                                   JsonNode states) {
}
