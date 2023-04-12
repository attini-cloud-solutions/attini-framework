package deployment.plan.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@EqualsAndHashCode
@RegisterForReflection
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ec2Configuration {

    private final CfnString instanceType;

    private final CfnString ecsClientLogGroup;

    private final CfnString instanceProfile;

    private final CfnString imageId;

    @JsonCreator
    public Ec2Configuration(CfnString instanceType, CfnString ecsClientLogGroup, CfnString instanceProfile, CfnString imageId) {
        this.instanceType = instanceType;
        this.ecsClientLogGroup = ecsClientLogGroup;
        this.instanceProfile = instanceProfile;
        this.imageId = imageId;
    }

    public CfnString getInstanceType() {
        return instanceType;
    }

    public CfnString getEcsClientLogGroup() {
        return ecsClientLogGroup;
    }

    public CfnString getInstanceProfile() {
        return instanceProfile;
    }

    public CfnString getImageId() {
        return imageId;
    }
}
