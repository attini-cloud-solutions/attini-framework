package attini.action.actions.deploycloudformation;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection

public class SfnExecutionArn {

    private final String value;

    private SfnExecutionArn(String value) {
        this.value = value;
    }

    public static SfnExecutionArn create(String value) {
        return new SfnExecutionArn(value);
    }

    @JsonValue
    public String asString() {
        return value;
    }

    public String extractExecutionId() {

        return value.substring(value.lastIndexOf(":") + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SfnExecutionArn that = (SfnExecutionArn) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
