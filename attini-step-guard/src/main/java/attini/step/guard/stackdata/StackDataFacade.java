/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.stackdata;

import attini.step.guard.cloudformation.CloudFormationEvent;
import attini.step.guard.cloudformation.InitDeploySnsEvent;
public interface StackDataFacade {


    InitDeployData getInitDeployData(String stackName);


    InitDeployData getInitDeployData(String stackName, String clientRequestToken);

    void removeInitTemplateMd5(String stackName);

    StackData getStackData(CloudFormationEvent cloudFormationEvent);

    void saveInitDeployError(InitDeploySnsEvent event, String error);

    void deleteCfnStack(CloudFormationEvent cloudFormationEvent);

    void deleteInitDeploy(InitDeploySnsEvent stepGuardInput);

    void saveError(CloudFormationEvent stepGuardInput, String error);

}
