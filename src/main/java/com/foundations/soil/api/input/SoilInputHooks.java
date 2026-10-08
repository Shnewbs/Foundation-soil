package com.foundations.soil.api.input;

import com.foundations.soil.FoundationsSoil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;

/** Generic external-delivery registry. 0.0.3a owns amendment acceptance, not transport infrastructure. */
public final class SoilInputHooks {
    private static final Map<SoilInputType, CopyOnWriteArrayList<Entry>> HANDLERS = new EnumMap<>(SoilInputType.class);
    private static final AtomicLong DISPATCHES = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();
    static {
        for (SoilInputType type : SoilInputType.values()) HANDLERS.put(type, new CopyOnWriteArrayList<>());
    }

    private SoilInputHooks() {}

    public static Registration register(ResourceLocation owner, SoilInputType type, int priority, SoilInputHandler handler) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        var list = HANDLERS.get(type);
        for (Entry existing : list) {
            if (existing.owner().equals(owner) && existing.priority() == priority && existing.handler() == handler) {
                return () -> { };
            }
        }
        Entry entry = new Entry(owner, priority, handler);
        list.add(entry);
        list.sort(Comparator.comparingInt(Entry::priority).reversed().thenComparing(e -> e.owner().toString()));
        return () -> list.remove(entry);
    }

    public static SoilInputResult dispatch(SoilExternalInput input) {
        Objects.requireNonNull(input, "input");
        DISPATCHES.incrementAndGet();
        for (Entry entry : HANDLERS.get(input.type())) {
            try {
                SoilInputResult result = entry.handler().handle(input);
                if (result != null && result.status() != SoilInputResult.Status.UNHANDLED) return result;
            } catch (Throwable error) {
                FAILURES.incrementAndGet();
                FoundationsSoil.LOGGER.warn("[Foundations Soil] External input handler {} failed for {}; continuing dispatch",
                    entry.owner(), input.type(), error);
            }
        }
        return SoilInputResult.unhandled();
    }

    public static int handlerCount() { return HANDLERS.values().stream().mapToInt(List::size).sum(); }
    public static int handlerCount(SoilInputType type) { return HANDLERS.get(type).size(); }
    public static long dispatchCount() { return DISPATCHES.get(); }
    public static long failureCount() { return FAILURES.get(); }

    public static Map<SoilInputType, Integer> counts() {
        Map<SoilInputType, Integer> result = new EnumMap<>(SoilInputType.class);
        for (SoilInputType type : SoilInputType.values()) result.put(type, HANDLERS.get(type).size());
        return Map.copyOf(result);
    }

    public static List<String> owners(SoilInputType type) {
        List<String> result = new ArrayList<>();
        for (Entry entry : HANDLERS.get(type)) result.add(entry.owner().toString());
        return List.copyOf(result);
    }

    private record Entry(ResourceLocation owner, int priority, SoilInputHandler handler) {}

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override void close();
    }
}
