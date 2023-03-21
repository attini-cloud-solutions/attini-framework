package attini.action.actions.deploycloudformation;

import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError.ACCESS_DENIED;
import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError.NO_STACK_EXISTS;
import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError.NO_UPDATE_TO_PERFORM;
import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError.ROLLBACK_COMPLETE;
import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError.UNKNOWN_ERROR;
import static attini.action.actions.deploycloudformation.CloudFormationErrorResolver.CloudFormationError.VALIDATION_ERROR;

import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

public class CloudFormationErrorResolver {

    public static CloudFormationError resolveError(CloudFormationException cloudFormationException) {
        if (isInRollBackCompleteState(cloudFormationException)) {
            return ROLLBACK_COMPLETE;
        }
        if (isInNoUpdateState(cloudFormationException)) {
            return NO_UPDATE_TO_PERFORM;
        }
        if (noStackExistsState(cloudFormationException)){
            return NO_STACK_EXISTS;
        }
        if (validationError(cloudFormationException)){
            return VALIDATION_ERROR;
        }

        if (accessDenied(cloudFormationException)){
            return ACCESS_DENIED;
        }
        return UNKNOWN_ERROR;
    }

    public enum CloudFormationError {
        ROLLBACK_COMPLETE, NO_UPDATE_TO_PERFORM, NO_STACK_EXISTS, VALIDATION_ERROR, UNKNOWN_ERROR, ACCESS_DENIED
    }

    private static boolean validationError(CloudFormationException e){
        return e.awsErrorDetails().errorCode().equals("ValidationError");
    }

    private static boolean noStackExistsState(CloudFormationException cloudFormationException){
        return cloudFormationException.awsErrorDetails()
                       .errorMessage()
                       .contains("does not exist");
    }

    private static boolean accessDenied(CloudFormationException cloudFormationException){
        return cloudFormationException.awsErrorDetails()
                                      .errorCode()
                                      .equals("AccessDenied");
    }

    private static boolean isInNoUpdateState(CloudFormationException cloudFormationException) {
        return cloudFormationException.awsErrorDetails().errorMessage().contentEquals("No updates are to be performed.");
    }

    private static boolean isInRollBackCompleteState(CloudFormationException e) {
        return e.awsErrorDetails()
                .errorMessage()
                .endsWith("is in ROLLBACK_COMPLETE state and can not be updated.");
    }
}
