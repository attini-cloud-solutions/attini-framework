package attini.step.guard.cdk;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CdkStack(String name, String id, CdkEnvironment environment) {
}
