package attini.action.actions.runner;

public class TaskStartFailedSyncException extends RuntimeException {

    public TaskStartFailedSyncException(String message) {
        super(message);
    }

    public TaskStartFailedSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
