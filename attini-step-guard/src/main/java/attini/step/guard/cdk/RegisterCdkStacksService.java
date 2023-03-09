package attini.step.guard.cdk;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.stream.Collectors;

import attini.step.guard.AttiniContext;
import attini.step.guard.EnvironmentVariables;
import attini.step.guard.stackdata.StackDataFacade;

public class RegisterCdkStacksService {

    private final StackDataFacade stackDataFacade;
    private final EnvironmentVariables environmentVariables;

    public RegisterCdkStacksService(StackDataFacade stackDataFacade,
                                    EnvironmentVariables environmentVariables) {
        this.stackDataFacade = requireNonNull(stackDataFacade, "stackDataFacade");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public Map<String, Object> registerStacks(RegisterCdkStacksEvent event) {

        AttiniContext context = AttiniContext.builder()
                                             .setDistributionId(event.distributionId())
                                             .setDistributionName(event.distributionName())
                                             .setEnvironment(event.environment())
                                             .setObjectIdentifier(event.objectIdentifier()).build();


        event.stacks()
             .stream()
             .map(cdkStack -> {
                 CdkEnvironment environment = cdkStack.environment();
                 String region = isUnknown(environment.region()) ? environmentVariables.getRegion() : environment.region();
                 String account = isUnknown(environment.account()) ? environmentVariables.getAccountId() : environment.account();
                 return new CdkStack(cdkStack.name(), cdkStack.id(), new CdkEnvironment(region, account));
             })
             .forEach(cdkStack -> stackDataFacade.saveCdkStack(context, event.stepName(), cdkStack));


        return event.stacks()
                    .stream()
                    .collect(Collectors.toMap(CdkStack::id, cdkStack ->{
                        long nrOfStacksWithName = event.stacks()
                                          .stream()
                                          .filter(cdkStack1 -> cdkStack1.name().equals(cdkStack.name()))
                                          .count();
                        if (nrOfStacksWithName > 1){
                            return "Could not resolve stack output, multiple stacks with same name detected in app";
                        }
                        return  event.outputs().get(cdkStack.name());
                    }));
    }

    private static boolean isUnknown(String value) {
        return value.contains("unknown-");
    }


}
