package attini.step.guard.cdk;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CdkEnvironment(String region, String account) {

}
