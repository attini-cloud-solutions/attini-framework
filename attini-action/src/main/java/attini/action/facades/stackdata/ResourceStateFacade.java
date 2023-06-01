/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.facades.stackdata;

import java.util.Optional;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import attini.action.actions.deploycloudformation.StackData;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.runner.RunnerData;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.domain.DeployOriginData;

public interface ResourceStateFacade {


    boolean acquireEc2StartLock(RunnerData runnerData);

    boolean acquireEcsStartLock(RunnerData runnerData);


    RunnerData getRunnerData(String stackName, String runnerName);

    void saveRunnerData(RunnerData runnerData);

    void saveManualApprovalData(DeploymentPlanExecutionMetadata metadata, DeployOriginData deployOriginData);

    void saveStackData(StackData stackData);

    void saveStackData(StackData stackData, String stackId);

    Optional<SfnExecutionArn> getStacksSfnExecutionArn(StackConfiguration stackConfiguration);

    void saveStackId(String stackId, StackConfiguration stackConfiguration);

    Optional<StackTemplate> getStackTemplate(StackConfiguration stackConfiguration);
}
