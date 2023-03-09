package attini.step.guard;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class InitDeployManualTriggerEvent {

    private final String stackName;

    public InitDeployManualTriggerEvent(String stackName) {
        this.stackName = requireNonNull(stackName, "stackName");
    }

    public String getStackName() {
        return stackName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitDeployManualTriggerEvent that = (InitDeployManualTriggerEvent) o;
        return Objects.equals(stackName, that.stackName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stackName);
    }
}
