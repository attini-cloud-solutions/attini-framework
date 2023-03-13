package attini.step.guard.cloudformation;

import org.jboss.logging.Logger;

public class CfnSnsEventTypeResolver {

    private static final Logger logger = Logger.getLogger(CfnSnsEventTypeResolver.class);


    public CfnEventType resolve(CloudFormationSnsEvent event){

        if (!isCloudFormationStack(event) && !CfnStatuses.isFailed(event.getResourceStatus())) {
            logger.debug("Not a Cloudformation event. Not responding.");
            return CfnEventType.RESOURCE_UPDATE;
        }

        if (!isCurrentStack(event) && !CfnStatuses.isFailed(event.getResourceStatus())){
            logger.debug("Logical Id does not match stack name. Not responding.");
            return CfnEventType.RESOURCE_UPDATE;
        }

        if (!isCurrentStack(event) && CfnStatuses.isFailed(event.getResourceStatus())){
            return CfnEventType.RESOURCE_FAILED;
        }

        if (isCurrentStack(event) && CfnStatuses.isUpdated(event.getResourceStatus())){
            return CfnEventType.STACK_UPDATED;
        }

        if (isCurrentStack(event) && CfnStatuses.isFailed(event.getResourceStatus())){
            return CfnEventType.STACK_FAILED;
        }

        if (isCurrentStack(event) && CfnStatuses.isDeleted(event.getResourceStatus())){
            return CfnEventType.STACK_DELETED;
        }


       return CfnEventType.STACK_IN_PROGRESS;

    }

    private static boolean isCloudFormationStack(CloudFormationSnsEvent event) {
        return event.getResourceType().contains("AWS::CloudFormation::Stack");
    }

    private static boolean isCurrentStack(CloudFormationSnsEvent event){
       return event.getStackName().equals(event.getLogicalResourceId());
    }
}
