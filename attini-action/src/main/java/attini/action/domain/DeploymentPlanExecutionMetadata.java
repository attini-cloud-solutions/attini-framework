package attini.action.domain;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record DeploymentPlanExecutionMetadata(int retryCounter,
                                              String sfnToken,
                                              String stepName,
                                              SfnExecutionArn executionArn,
                                              String executionStartTime) {
}
