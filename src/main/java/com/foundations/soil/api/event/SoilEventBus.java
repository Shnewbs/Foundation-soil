package com.foundations.soil.api.event;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight process-lifetime event bus for Soil integrations.
 * Listeners execute synchronously on the thread that performs the authoritative Soil operation.
 * R8 isolates listener failures and records per-event diagnostics.
 */
public final class SoilEventBus {
    private static final Logger LOGGER = Logger.getLogger("FoundationsSoilEvents");
    private final Map<Class<?>, CopyOnWriteArrayList<Listener<?>>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, AtomicLong> postedByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, AtomicLong> failuresByType = new ConcurrentHashMap<>();
    private final AtomicLong posted = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    public <T extends SoilEvent> Subscription listen(Class<T> type, Consumer<T> consumer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(consumer, "consumer");
        var list = listeners.computeIfAbsent(type, ignored -> new CopyOnWriteArrayList<>());
        for (Listener<?> existing : list) {
            if (existing.consumer() == consumer) {
                return () -> { };
            }
        }
        Listener<T> listener = new Listener<>(consumer);
        list.add(listener);
        return () -> {
            var current = listeners.get(type);
            if (current != null) {
                current.remove(listener);
                if (current.isEmpty()) listeners.remove(type, current);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <T extends SoilEvent> void post(T event) {
        Objects.requireNonNull(event, "event");
        Class<?> eventType = event.getClass();
        posted.incrementAndGet();
        postedByType.computeIfAbsent(eventType, ignored -> new AtomicLong()).incrementAndGet();

        var list = listeners.get(eventType);
        if (list == null) return;
        for (Listener<?> raw : list) {
            try {
                ((Listener<T>) raw).consumer().accept(event);
            } catch (Throwable error) {
                failures.incrementAndGet();
                failuresByType.computeIfAbsent(eventType, ignored -> new AtomicLong()).incrementAndGet();
                LOGGER.log(Level.WARNING,
                    "Foundations Soil integration listener failed for " + eventType.getSimpleName() + "; continuing dispatch",
                    error);
            }
        }
    }

    public int listenerCount() {
        return listeners.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
    }

    public int listenerCount(Class<? extends SoilEvent> type) {
        var list = listeners.get(type);
        return list == null ? 0 : list.size();
    }

    public long postedCount() { return posted.get(); }
    public long failureCount() { return failures.get(); }

    /** Active listener counts by event type. */
    public Map<String, Integer> listenerCounts() {
        Map<String, Integer> result = new TreeMap<>();
        listeners.forEach((type, list) -> result.put(type.getSimpleName(), list.size()));
        return Map.copyOf(result);
    }

    /** Posted event counts by event type. This is the map shown by /fsoil hooks. */
    public Map<String, Long> postedCounts() {
        Map<String, Long> result = new TreeMap<>();
        postedByType.forEach((type, count) -> result.put(type.getSimpleName(), count.get()));
        return Map.copyOf(result);
    }

    /** Listener failure counts by event type. */
    public Map<String, Long> failureCounts() {
        Map<String, Long> result = new TreeMap<>();
        failuresByType.forEach((type, count) -> result.put(type.getSimpleName(), count.get()));
        return Map.copyOf(result);
    }

    private record Listener<T extends SoilEvent>(Consumer<T> consumer) {}

    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override void close();
    }
}
