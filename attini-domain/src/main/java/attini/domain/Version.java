package attini.domain;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;


@RegisterForReflection
public class Version {

    private final String value;

    private Version(String value) {
        this.value = requireNonNull(value, "value");
    }


    @JsonValue
    public String asString() {
        return value;
    }

    public static Version of(String value){
        return new Version(value);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return Objects.equals(value, version.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Version{" +
               "value='" + value + '\'' +
               '}';
    }
}
