package attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DistributionName {

    private final String value;

    private DistributionName(String value) {
        this.value = requireNonNull(value, "value");
    }
    public static DistributionName of(String value){
        return new DistributionName(value);
    }

    @JsonValue
    public String asString(){
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributionName that = (DistributionName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DistributionName{" +
               "value='" + value + '\'' +
               '}';
    }
}
