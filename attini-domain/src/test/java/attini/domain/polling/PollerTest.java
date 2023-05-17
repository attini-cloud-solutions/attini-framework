package attini.domain.polling;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class PollerTest {


    @Test
    void call3Times() {

        AtomicInteger counter = new AtomicInteger();

        Poller.builder(() -> new PollingResult<>(counter.incrementAndGet() == 3, counter.get()))
              .setCalls(3)
              .setInterval(2, TimeUnit.MILLISECONDS)
              .build()
              .poll();

        assertEquals(3, counter.get());
    }

    @Test
    void shouldReturnSupplierValue(){
        AtomicInteger counter = new AtomicInteger();

        Integer value = Poller.builder(() -> new PollingResult<>(counter.incrementAndGet() == 3, counter.get()))
                             .setCalls(3)
                             .setInterval(2, TimeUnit.MILLISECONDS)
                             .build()
                             .poll();

        assertEquals(3,value);
    }

    @Test
    void shouldThrowSuppliedException() {


        assertThrows(IllegalArgumentException.class,
                     () -> Poller.builder(() -> new PollingResult<>(false, false))
                                 .setCalls(3)
                                 .setInterval(2, TimeUnit.MILLISECONDS)
                                 .setTimeoutExceptionSupplier(() -> new IllegalArgumentException(""))
                                 .build()
                                 .poll());


    }
}
