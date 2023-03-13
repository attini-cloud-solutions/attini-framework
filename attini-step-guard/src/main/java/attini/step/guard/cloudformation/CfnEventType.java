package attini.step.guard.cloudformation;


/**
 * Represent the CloudFormation event type that has been triggered.
 * <p>
 * RESOURCE = An event from a resource that is managed by the stack, ex a Lambda function.
 * STACK = An event from the stack.
 */
public enum CfnEventType {

    RESOURCE_FAILED,
    RESOURCE_UPDATE,
    STACK_FAILED,
    STACK_UPDATED,
    STACK_DELETED,
    STACK_IN_PROGRESS,

}
