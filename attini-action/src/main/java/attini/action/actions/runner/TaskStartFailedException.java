package attini.action.actions.runner;

public class TaskStartFailedException extends RuntimeException{

    private final TaskStatus taskStatus;

    public TaskStartFailedException(TaskStatus taskStatus) {
        super(taskStatus.stopReason());
        this.taskStatus = taskStatus;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }
}
