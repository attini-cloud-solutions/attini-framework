package attini.action.actions.runner;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Ec2Config(String instanceType, String ecsClientLogGroup, String instanceProfile) {
}
