package com.foundations.soil.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;

/** Process-lifetime registry used for transparent integration diagnostics. */
public final class SoilIntegrationRegistry {
    private static final Map<ResourceLocation, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final AtomicLong DUPLICATE_REGISTRATIONS = new AtomicLong();

    private SoilIntegrationRegistry() {}

    public static void register(ResourceLocation id, String displayName, Set<String> capabilities) {
        if (id == null) throw new IllegalArgumentException("integration id cannot be null");
        ENTRIES.compute(id, (key, old) -> {
            if (old == null) return new Entry(displayName, capabilities);
            String normalizedName = normalizeName(displayName);
            Set<String> normalizedCapabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
            if (old.displayName.equals(normalizedName) && old.capabilities.equals(normalizedCapabilities)) {
                DUPLICATE_REGISTRATIONS.incrementAndGet();
                return old;
            }
            old.update(normalizedName, normalizedCapabilities);
            return old;
        });
    }

    public static void register(String id, String displayName, String... capabilities) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) throw new IllegalArgumentException("Invalid integration id: " + id);
        register(parsed, displayName, capabilities == null ? Set.of() : Set.copyOf(Arrays.asList(capabilities)));
    }

    public static void record(ResourceLocation id, String kind) {
        Entry entry = ENTRIES.get(id);
        if (entry == null) {
            register(id, id.toString(), Set.of());
            entry = ENTRIES.get(id);
        }
        entry.counters.computeIfAbsent(normalize(kind), ignored -> new AtomicInteger()).incrementAndGet();
    }

    public static int size() { return ENTRIES.size(); }
    public static long duplicateRegistrationsSuppressed() { return DUPLICATE_REGISTRATIONS.get(); }

    public static Map<ResourceLocation, Snapshot> snapshot() {
        Map<ResourceLocation, Snapshot> result = new TreeMap<>((a, b) -> a.toString().compareTo(b.toString()));
        ENTRIES.forEach((id, entry) -> result.put(id, entry.snapshot()));
        return Collections.unmodifiableMap(result);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "operations" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeName(String value) {
        return value == null || value.isBlank() ? "Integration" : value.trim();
    }

    public record Snapshot(String displayName, Set<String> capabilities, Map<String, Integer> counters) {}

    private static final class Entry {
        volatile String displayName;
        volatile Set<String> capabilities;
        final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

        Entry(String displayName, Set<String> capabilities) { update(displayName, capabilities); }

        void update(String displayName, Set<String> capabilities) {
            this.displayName = normalizeName(displayName);
            this.capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
        }

        Snapshot snapshot() {
            Map<String, Integer> counts = new TreeMap<>();
            counters.forEach((key, value) -> counts.put(key, value.get()));
            return new Snapshot(displayName, capabilities, Map.copyOf(counts));
        }
    }
}
