package attini.action.actions.runner;

import java.util.Objects;
import java.util.Optional;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;

@RegisterForReflection
@Builder(builderClassName = "Builder")
public final class Ec2Config {
    private final String instanceType;
    private final String ecsClientLogGroup;
    private final String instanceProfile;
    private final String imageId;



    public Optional<String> imageId() {
        return Optional.ofNullable(imageId);
    }

    public String instanceType() {
        return instanceType;
    }

    public String ecsClientLogGroup() {
        return ecsClientLogGroup;
    }

    public String instanceProfile() {
        return instanceProfile;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Ec2Config) obj;
        return Objects.equals(this.instanceType, that.instanceType) &&
               Objects.equals(this.ecsClientLogGroup, that.ecsClientLogGroup) &&
               Objects.equals(this.instanceProfile, that.instanceProfile) &&
               Objects.equals(this.imageId, that.imageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instanceType, ecsClientLogGroup, instanceProfile, imageId);
    }

    @Override
    public String toString() {
        return "Ec2Config[" +
               "instanceType=" + instanceType + ", " +
               "ecsClientLogGroup=" + ecsClientLogGroup + ", " +
               "instanceProfile=" + instanceProfile + ", " +
               "imageId=" + imageId + ']';
    }

}
