package attini.domain.polling;

public record PollingResult<V>(boolean stopPolling,
                               V result) {

    public PollingResult(boolean stopPolling) {
        this(stopPolling, null);
    }

    public PollingResult(boolean stopPolling, V result) {
        this.stopPolling = stopPolling;
        this.result = result;
    }
}
