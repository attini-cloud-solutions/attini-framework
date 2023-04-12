package deployment.plan.transform;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@Builder(toBuilder = true)
@EqualsAndHashCode
@RegisterForReflection
@ToString
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class Runner {

    private final String name;
    private final CfnString subnets;
    private final CfnString securityGroups;
    private final CfnString assignPublicIp;
    private final CfnString maxConcurrentJobs;
    private final CfnString idleTimeToLive;
    private final CfnString jobTimeout;
    private final CfnString logLevel;
    private final CfnString taskDefinitionArn;
    private final CfnString containerName;
    private final CfnString cluster;
    private final CfnString queueUrl;
    private final CfnString roleArn;
    private final List<CfnString> installationCommands;
    private final CfnString installationCommandsTimeout;
    private final Ec2Configuration ec2Configuration;

    private final CfnString cpu;
    private final CfnString memory;


    public String getName() {
        return name;
    }

    public CfnString getSubnets() {
        return subnets;
    }

    public CfnString getSecurityGroups() {
        return securityGroups;
    }

    public CfnString getAssignPublicIp() {
        return assignPublicIp;
    }

    public CfnString getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    public CfnString getIdleTimeToLive() {
        return idleTimeToLive;
    }

    public CfnString getJobTimeout() {
        return jobTimeout;
    }

    public CfnString getLogLevel() {
        return logLevel;
    }

    public CfnString getTaskDefinitionArn() {
        return taskDefinitionArn;
    }

    public CfnString getContainerName() {
        return containerName;
    }

    public CfnString getCluster() {
        return cluster;
    }


    public CfnString getQueueUrl() {
        return queueUrl;
    }


    public CfnString getRoleArn() {
        return roleArn;
    }

    public CfnString getInstallationCommandsTimeout() {
        return installationCommandsTimeout;
    }


    public List<CfnString> getInstallationCommands() {
        return installationCommands == null ? Collections.emptyList() : installationCommands;
    }

    public Optional<Ec2Configuration> getEc2Configuration() {
        return Optional.ofNullable(ec2Configuration);
    }

    public CfnString getCpu() {
        return cpu;
    }

    public CfnString getMemory() {
        return memory;
    }
}
