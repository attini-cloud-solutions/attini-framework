package attini.step.guard.cloudformation;

import java.util.Set;

public class CfnStatuses {

    private static final Set<String> FAILED_RESPONSE_CFN_STATUS = Set.of(
            "CREATE_FAILED",
            "UPDATE_FAILED",
            "ROLLBACK_FAILED",
            "ROLLBACK_COMPLETE",
            "DELETE_FAILED",
            "UPDATE_ROLLBACK_FAILED",
            "UPDATE_ROLLBACK_COMPLETE"
    );
    private static final Set<String> UPDATED_STATUSES = Set.of(
            "UPDATE_COMPLETE",
            "CREATE_COMPLETE"
    );

    public static boolean isFailed(String status){
       return FAILED_RESPONSE_CFN_STATUS.contains(status);
    }

    public static boolean isUpdated(String status){
       return UPDATED_STATUSES.contains(status);
    }

    public static boolean isDeleted(String status){
        return "DELETE_COMPLETE".equals(status);
    }

}
