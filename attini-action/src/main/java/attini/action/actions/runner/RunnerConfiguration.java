package attini.action.actions.runner;

import java.util.Objects;
import java.util.Optional;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class RunnerConfiguration {
    private final Integer maxConcurrentJobs;
    private final Integer idleTimeToLive;
    private final Integer jobTimeout;
    private final LogLevel logLevel;

    public Optional<Integer> getMaxConcurrentJobs() {
        return Optional.ofNullable(maxConcurrentJobs);
    }

    public Optional<Integer> getIdleTimeToLive() {
        return Optional.ofNullable(idleTimeToLive);
    }

    public Optional<Integer> getJobTimeout() {
        return Optional.ofNullable(jobTimeout);
    }

    public Optional<LogLevel> getLogLevel() {
        return Optional.ofNullable(logLevel);
    }

    public enum LogLevel{
        DEBUG, INFO, WARN, ERROR, OFF, ALL
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunnerConfiguration that = (RunnerConfiguration) o;
        return Objects.equals(maxConcurrentJobs, that.maxConcurrentJobs) && Objects.equals(
                idleTimeToLive,
                that.idleTimeToLive) && Objects.equals(jobTimeout, that.jobTimeout) && logLevel == that.logLevel;
    }


    //We need to our own hash code implementation here because enums Hash code is not guaranteed to be consistent between different JREs
    @Override
    public int hashCode() {
        return Objects.hash(maxConcurrentJobs, idleTimeToLive, jobTimeout, logLevel == null ? null : logLevel.name());
    }
}
