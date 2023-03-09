package attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;


@RegisterForReflection
public class DistributionId {

    private final String value;

    private DistributionId(String value) {
        this.value = requireNonNull(value, "value");
    }

    public static DistributionId of(String value) {
        return new DistributionId(value);
    }

    @JsonValue
    public String asString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributionId that = (DistributionId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DistributionId{" +
               "value='" + value + '\'' +
               '}';
    }
}
