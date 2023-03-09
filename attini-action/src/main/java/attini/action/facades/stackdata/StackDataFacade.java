/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.facades.stackdata;

import java.util.Optional;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import attini.domain.DeployOriginData;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.deploycloudformation.StackData;
import attini.action.actions.runner.RunnerData;
import attini.action.domain.DeploymentPlanExecutionMetadata;

public interface StackDataFacade {

    RunnerData getRunnerData(String stackName, String runnerName, boolean consistentRead);

    RunnerData getRunnerData(String stackName, String runnerName);

    void saveRunnerData(RunnerData runnerData);

    void saveManualApprovalData(DeploymentPlanExecutionMetadata metadata, DeployOriginData deployOriginData);

    void saveStackData(StackData stackData);

    void saveStackData(StackData stackData, String stackId);

    Optional<SfnExecutionArn> getStacksSfnExecutionArn(StackConfiguration stackConfiguration);

    void saveToken(String sfnToken, StackConfiguration stackConfiguration);
}
