package attini.action.actions.deploycloudformation;

public record StackStatus(String clientRequestToken,
                          StackState stackState,
                          String stackStatus,
                          String stackId) {

    public enum StackState {
        UPDATE_IN_PROGRESS, COMPLETE
    }

}
