package attini.action.actions.runner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;


@Builder
@EqualsAndHashCode
@ToString
public final class TaskConfiguration {
    @NonNull
    private final String taskDefinitionArn;

    private final String cluster;
    @NonNull
    private final String assignPublicIp;
    @NonNull
    private final String queueUrl;
    @NonNull
    private final RunnerConfiguration runnerConfiguration;
    @NonNull
    private final Set<String> subnets;

    private final String attiniVersion;

    private final Set<String> securityGroups;
    private final String container;
    private final List<String> installationCommands;
    private final Integer installationCommandsTimeout;

    private final String roleArn;

    private final Integer cpu;
    private final Integer memory;


    public Set<String> subnets() {
        return subnets;
    }

    public String taskDefinitionArn() {
        return taskDefinitionArn;
    }

    public String cluster() {
        return cluster == null ? "attini-default" : cluster ;
    }
    public String assignPublicIp() {
        return assignPublicIp;
    }

    public Set<String> securityGroups() {
        return securityGroups == null ? Collections.emptySet() : securityGroups;
    }

    public Optional<String> container() {
        return Optional.ofNullable(container);
    }

    public Optional<Integer> getInstallationCommandsTimeout() {
        return Optional.ofNullable(installationCommandsTimeout);
    }

    public String queueUrl() {
        return queueUrl;
    }

    public RunnerConfiguration runnerConfiguration() {
        return runnerConfiguration;
    }

    public List<String> installationCommands() {
        return installationCommands == null ? Collections.emptyList() : installationCommands;
    }

    public String attiniVersion() {
        return attiniVersion;
    }

    public Optional<String> roleArn() {
        return Optional.ofNullable(roleArn);
    }

    public Optional<Integer> cpu() {
        return Optional.ofNullable(cpu);
    }

    public Optional<Integer> memory() {
        return Optional.ofNullable(memory);
    }
}
