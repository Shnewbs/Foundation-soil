package com.foundations.soil;

import com.foundations.soil.api.event.SoilEvent;
import com.foundations.soil.api.event.SoilEventBus;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoilEventBusTest {
    private record TestEvent(int value) implements SoilEvent {}

    @Test
    void subscriptionReceivesThenCleanlyUnsubscribes() {
        SoilEventBus bus = new SoilEventBus();
        AtomicInteger total = new AtomicInteger();
        var subscription = bus.listen(TestEvent.class, event -> total.addAndGet(event.value()));

        bus.post(new TestEvent(4));
        assertEquals(4, total.get());
        assertEquals(1, bus.listenerCount(TestEvent.class));
        assertEquals(1L, bus.postedCount());
        assertEquals(1L, bus.postedCounts().get("TestEvent"));

        subscription.close();
        bus.post(new TestEvent(8));
        assertEquals(4, total.get());
        assertEquals(0, bus.listenerCount(TestEvent.class));
        assertEquals(2L, bus.postedCount());
        assertEquals(2L, bus.postedCounts().get("TestEvent"));
    }

    @Test
    void badListenerDoesNotPreventFollowingListenerAndIsCounted() {
        SoilEventBus bus = new SoilEventBus();
        AtomicInteger calls = new AtomicInteger();
        bus.listen(TestEvent.class, event -> { throw new IllegalStateException("test"); });
        bus.listen(TestEvent.class, event -> calls.incrementAndGet());
        assertDoesNotThrow(() -> bus.post(new TestEvent(1)));
        assertEquals(1, calls.get());
        assertEquals(1L, bus.failureCount());
        assertEquals(1L, bus.failureCounts().get("TestEvent"));
    }

    @Test
    void sameConsumerInstanceIsNotRegisteredTwice() {
        SoilEventBus bus = new SoilEventBus();
        AtomicInteger calls = new AtomicInteger();
        Consumer<TestEvent> consumer = event -> calls.incrementAndGet();
        bus.listen(TestEvent.class, consumer);
        bus.listen(TestEvent.class, consumer);
        bus.post(new TestEvent(1));
        assertEquals(1, calls.get());
        assertEquals(1, bus.listenerCount(TestEvent.class));
    }
}
