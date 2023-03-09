package deployment.plan.transform;


import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomStringDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@RegisterForReflection
public class DeploymentPlan {

    private final String startAt;
    private final Map<String, Map<String, Object>> states;


    @JsonCreator
    public DeploymentPlan(@JsonProperty("StartAt") @JsonDeserialize(using = CustomStringDeserializer.class) String startAt,
                          @JsonProperty("States") @JsonDeserialize(using = StatesDeserializer.class) Map<String, Map<String, Object>> states) {
        if (states == null){
            throw new IllegalArgumentException("States is missing in the deployment plan, please define at least one state/step");
        }

        if (startAt == null){
            throw new IllegalArgumentException("StartAt is missing in the deployment plan, please define with what step the deployment plan should start");
        }
        this.startAt = startAt;
        this.states = states;
    }

    @JsonProperty("StartAt")
    public String getStartAt() {
        return startAt;
    }

    @JsonProperty("States")
    public Map<String, Map<String, Object>> getStates() {
        return states;
    }
}
