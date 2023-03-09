package attini.step.guard;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import attini.step.guard.stackdata.DesiredState;

public class StepGuardInputBuilder {

    public static CloudFormationManualTriggerEvent.Builder aManualTrigger() {
        return CloudFormationManualTriggerEvent.builder()
                                               .setStackName("stackName")
                                               .setResourceStatus("UPDATE_COMPLETE")
                                               .setRegion("region")
                                               .setExecutionRoleArn("executionRoleArn")
                                               .setStackId("stackId")
                                               .setSfnResponseToken("sfnResponseToken")
                                               .setStepName("stepName")
                                               .setOutputPath("outputPath")
                                               .setClientRequestToken("clientRequestToken")
                                               .setLogicalResourceId("an-id")
                                               .setDesiredState(DesiredState.DEPLOYED)
                                               .setAttiniContext(AttiniContext.builder()
                                                                              .setDistributionId(DistributionId.of("distributionId"))
                                                                              .setDistributionName(DistributionName.of("distributionName"))
                                                                              .setEnvironment(Environment.of("environment"))
                                                                              .setObjectIdentifier(ObjectIdentifier.of("objectIdentifier"))
                                                                              .build());
    }

    public static CloudFormationSnsEvent.Builder aSnsTrigger() {
        return CloudFormationSnsEvent.builder()
                                     .setStackName("StackName")
                                     .setResourceStatus("UPDATE_COMPLETE")
                                     .setLogicalResourceId("StackName")
                                     .setResourceType("AWS::CloudFormation::Stack")
                                     .setClientRequestToken("ClientRequestToken")
                                     .setStackId("StackId");

    }
}
