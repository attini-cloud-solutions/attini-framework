package attini.action.actions.runner;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import software.amazon.awssdk.services.ecs.model.TaskStopCode;

final class TaskStatus {
    private static final List<String> SHUT_DOWN_STATUSES = List.of("DEACTIVATING",
                                                                   "STOPPING",
                                                                   "DEPROVISIONING",
                                                                   "STOPPED");
    private static final List<String> STARTING_STATUSES = List.of("PROVISIONING", "PENDING", "ACTIVATING");
    private final String desiredStatus;
    private final String lastStatus;
    private final TaskStopCode stopCode;
    private final String stopReason;

    TaskStatus(String desiredStatus,
               String lastStatus,
               TaskStopCode stopCode,
               String stopReason) {
        this.desiredStatus = desiredStatus;
        this.lastStatus = lastStatus;
        this.stopCode = stopCode;
        this.stopReason = stopReason;
    }

    public boolean isStoppingOrStopped() {
        return SHUT_DOWN_STATUSES.contains(desiredStatus);
    }

    public boolean isStarting() {
        return lastStatus != null && STARTING_STATUSES.contains(lastStatus);
    }

    public boolean isRunning() {
        return "RUNNING".equals(lastStatus);
    }

    public boolean isDead() {
        return "STOPPED".equals(desiredStatus) && lastStatus == null && stopCode == null && stopReason == null;
    }

    public static TaskStatus deadTask() {
        return new TaskStatus("STOPPED", null, null, null);
    }

    public String lastStatus() {
        return lastStatus;
    }

    public Optional<TaskStopCode> stopCode() {
        return Optional.ofNullable(stopCode);
    }

    public String stopReason() {
        return stopReason;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TaskStatus) obj;
        return Objects.equals(this.desiredStatus, that.desiredStatus) &&
               Objects.equals(this.lastStatus, that.lastStatus) &&
               Objects.equals(this.stopCode, that.stopCode) &&
               Objects.equals(this.stopReason, that.stopReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(desiredStatus, lastStatus, stopCode, stopReason);
    }

    @Override
    public String toString() {
        return "TaskStatus[" +
               "desiredStatus=" + desiredStatus + ", " +
               "lastStatus=" + lastStatus + ", " +
               "stopCode=" + stopCode + ", " +
               "stopReason=" + stopReason + ']';
    }

}
