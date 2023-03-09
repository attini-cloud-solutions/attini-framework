package attini.step.guard;

import java.util.Optional;

public interface CloudFormationEvent {

    String getStackName();

    Optional<String> getStackId();

    Optional<String> getExecutionRoleArn();

    Optional<String> getRegion();

    String getClientRequestToken();

    String getResourceStatus();

    String getLogicalResourceId();

}
