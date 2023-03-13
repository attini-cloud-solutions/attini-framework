package attini.step.guard.cloudformation;

public interface CloudFormationSnsEvent extends CloudFormationEvent {


    String getResourceType();
}
