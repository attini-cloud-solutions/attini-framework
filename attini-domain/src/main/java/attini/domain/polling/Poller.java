package attini.domain.polling;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Poller <V> {

    private final int calls;
    private final TimeUnit timeUnit;
    private final int interval;
    private final Supplier<RuntimeException> exceptionSupplier;

    private final Supplier<PollingResult<V>> action;

    private Poller(Builder<V> builder) {
        this.calls = Objects.requireNonNull(builder.calls, "calls");
        this.timeUnit = Objects.requireNonNull(builder.timeUnit, "timeUnit");
        this.interval = Objects.requireNonNull(builder.interval, "interval");
        this.exceptionSupplier = builder.timeoutExceptionSupplier;
        this.action = builder.action;
    }

    public static <V> Builder<V> builder(Supplier<PollingResult<V>> action) {
        return new Builder<>(action);
    }



    public V poll(){

        for (int i = 0; i < calls; i++) {
            waitForInterval();
            PollingResult<V> vPollingResult = action.get();
            if (vPollingResult.stopPolling()){
                return vPollingResult.result();
            }
        }


       if (exceptionSupplier != null){
           throw exceptionSupplier.get();
       }

        throw new PollerTimeoutException("The poller timed out and no exception was suppler was provided. ");
    }

    private void waitForInterval() {
        try {
            timeUnit.sleep(interval);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static class Builder <V> {
        private Integer calls;
        private TimeUnit timeUnit;
        private Integer interval;

        private Supplier<RuntimeException> timeoutExceptionSupplier;

        private final Supplier<PollingResult<V>> action;

        private Builder(Supplier<PollingResult<V>> action) {
            this.action = action;
        }

        public Builder<V> setCalls(int calls) {
            this.calls = calls;
            return this;
        }


        public Builder<V> setInterval(int interval, TimeUnit timeUnit) {
            this.interval = interval;
            this.timeUnit = timeUnit;
            return this;
        }

        public Builder<V> setTimeoutExceptionSupplier(Supplier<RuntimeException> exceptionSupplier) {
            this.timeoutExceptionSupplier = exceptionSupplier;
            return this;
        }

        public Poller<V> build() {
            return new Poller<>(this);
        }
    }
}
