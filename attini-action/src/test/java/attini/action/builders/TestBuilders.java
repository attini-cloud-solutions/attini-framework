/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.builders;

import attini.action.actions.deploycloudformation.SfnExecutionArn;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.deploycloudformation.StackData;
import attini.action.domain.CfnStackConfig;
import attini.action.domain.DeploymentPlanExecutionMetadata;
import attini.action.domain.DesiredState;
import attini.action.domain.Distribution;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class TestBuilders {

    public static StackData.Builder aStackData() {
        return StackData.builder()
                        .setObjectIdentifier(ObjectIdentifier.of("SomeEtag12323"))
                        .setDistributionId(DistributionId.of("06496df7-194f-46e6-b250-c0c2a7fcf97d"))
                        .setDistributionName(DistributionName.of("infra"))
                        .setEnvironment(Environment.of("dev"))
                        .setDeploymentPlanExecutionMetadata(aMetaData())
                        .setStackConfiguration(aStackConfig().build());
    }

    public static StackConfiguration.Builder aStackConfig() {
        return StackConfiguration.builder()
                                 .setTemplate("some/path")
                                 .setStackName("my-favorite-stack")
                                 .setDesiredState(DesiredState.DEPLOYED);
    }


    public static CfnStackConfig.Builder aCfnStackConfig() {
        return CfnStackConfig.builder()
                             .setObjectIdentifier(ObjectIdentifier.of("SomeEtag12323"))
                             .setStackName("my-favorite-stack")
                             .setDistributionId(DistributionId.of("06496df7-194f-46e6-b250-c0c2a7fcf97d"))
                             .setDistributionName(DistributionName.of("infra"))
                             .setEnvironment(Environment.of("dev"))
                             .setStackRole("arn:aws:iam::655047308345:role/dev-test-cfn-role-eu-west-1")
                             .setTemplate("/some/path")
                             .setTemplateUrlPrefix("/s3/path").setInitStackName("my-init-stack");
    }

    public static DeploymentPlanExecutionMetadata aMetaData() {
        return aMetaData(SfnExecutionArn.create("arn:aws:states:eu-west-1:655047308345:execution:PipelineAttiniDeploymentPlanSfn-JrVqujOxtZcG:03e10a98-86fe-466e-8f05"));
    }

    public static DeploymentPlanExecutionMetadata aMetaData(SfnExecutionArn sfnExecutionArn) {
        return new DeploymentPlanExecutionMetadata(0,
                                                   "aVeryShortToken",
                                                   "myStep",
                                                   sfnExecutionArn,
                                                   "12232323231312312L");
    }

    public static Distribution.DistributionBuilder aDistribution() {
        return Distribution.builder()
                           .distributionId(DistributionId.of("06496df7-194f-46e6-b250-c0c2a7fcf97d"))
                           .deploymentSourcePrefix("a-prefix")
                           .distributionName(DistributionName.of("infra"))
                           .environment(Environment.of("dev"));
    }
}
