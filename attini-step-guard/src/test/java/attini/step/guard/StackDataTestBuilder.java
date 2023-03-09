package attini.step.guard;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.StackData;

public class StackDataTestBuilder {

    public static StackData.Builder aStackData(){
        return StackData.builder()
                 .setSfnToken("a-token")
                 .setStackId("1232321321")
                 .setStepName("my-step")
                 .setEnvironment(Environment.of("dev"))
                 .setDistributionId(DistributionId.of("123232-23232123"))
                 .setDistributionName(DistributionName.of("my-dist"))
                 .setObjectIdentifier(ObjectIdentifier.of("an/id:12322222"))
                 .setDesiredState(DesiredState.DEPLOYED);
    }
}
