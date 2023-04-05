package attini.action.actions.runner;

import java.util.Optional;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Ec2 {

    private final Ec2Config ec2Config;

    private final String latestEc2InstanceId;

    private final String imageId;

    private final int configHashCode;



    public Ec2Config getEc2Config() {
        return ec2Config;
    }

    public Optional<String> getLatestEc2InstanceId() {
        return Optional.ofNullable(latestEc2InstanceId);
    }

    public int getConfigHashCode() {
        return configHashCode;
    }

    public Optional<String> getImageId() {
        return Optional.ofNullable(imageId);
    }
}
