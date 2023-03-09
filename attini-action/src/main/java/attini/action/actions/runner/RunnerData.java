package attini.action.actions.runner;

import java.util.Optional;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class RunnerData {

    private final DistributionId distributionId;
    private final DistributionName distributionName;
    private final Environment environment;
    private final String objectIdentifier;
    private final String stackName;
    private final String runnerName;
    private final String taskId;
    private final String container;
    private final String taskDefinitionArn;
    private final Integer taskConfigurationHashCode;
    /**
     * Previous cluster. Will normally be the same as the cluster
     * in TaskConfiguration but can differ if the user changes the cluster.
     * If so then we need to use this cluster to stop the old task.
     */
    private final String cluster;
    private final TaskConfiguration taskConfiguration;
    private final boolean started;

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getStackName() {
        return stackName;
    }

    public String getRunnerName() {
        return runnerName;
    }

    public Optional<String> getTaskId() {
        return Optional.ofNullable(taskId);
    }

    public Optional<String> getContainer() {
        return Optional.ofNullable(container);
    }

    public String getTaskDefinitionArn() {
        return taskDefinitionArn;
    }

    public Integer getTaskConfigurationHashCode() {
        return taskConfigurationHashCode;
    }

    public String getCluster() {
        return cluster == null ? "attini-default" : cluster ;
    }

    public TaskConfiguration getTaskConfiguration() {
        return taskConfiguration;
    }

    public boolean isStarted() {
        return started;
    }

}
