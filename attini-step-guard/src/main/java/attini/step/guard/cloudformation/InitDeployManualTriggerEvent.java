package attini.step.guard.cloudformation;

import static java.util.Objects.requireNonNull;

public record InitDeployManualTriggerEvent(String stackName) {

    public InitDeployManualTriggerEvent(String stackName) {
        this.stackName = requireNonNull(stackName, "stackName");
    }

}
