package com.foundations.soil.diagnostic;

import java.util.concurrent.atomic.AtomicLong;

/** Lightweight runtime counters used by /fsoil status and R8 self-test diagnostics. */
public final class SoilRuntimeMetrics {
    private static final AtomicLong TOTAL_BATCHES = new AtomicLong();
    private static final AtomicLong TOTAL_CELLS_ATTEMPTED = new AtomicLong();
    private static final AtomicLong TOTAL_CELLS_CHANGED = new AtomicLong();
    private static final AtomicLong TOTAL_SIMULATION_NANOS = new AtomicLong();

    private static volatile long lastDay = -1L;
    private static volatile int lastDayCellsAttempted;
    private static volatile int lastDayCellsChanged;
    private static volatile long lastBatchNanos;
    private static volatile long slowestBatchNanos;

    private SoilRuntimeMetrics() {}

    public static synchronized void recordBatch(long day, int attempted, int changed, long nanos) {
        TOTAL_BATCHES.incrementAndGet();
        TOTAL_CELLS_ATTEMPTED.addAndGet(Math.max(0, attempted));
        TOTAL_CELLS_CHANGED.addAndGet(Math.max(0, changed));
        TOTAL_SIMULATION_NANOS.addAndGet(Math.max(0L, nanos));
        if (lastDay != day) {
            lastDay = day;
            lastDayCellsAttempted = 0;
            lastDayCellsChanged = 0;
        }
        lastDayCellsAttempted += Math.max(0, attempted);
        lastDayCellsChanged += Math.max(0, changed);
        lastBatchNanos = Math.max(0L, nanos);
        slowestBatchNanos = Math.max(slowestBatchNanos, lastBatchNanos);
    }

    public static Snapshot snapshot() {
        return new Snapshot(
            lastDay,
            lastDayCellsAttempted,
            lastDayCellsChanged,
            TOTAL_BATCHES.get(),
            TOTAL_CELLS_ATTEMPTED.get(),
            TOTAL_CELLS_CHANGED.get(),
            lastBatchNanos,
            slowestBatchNanos,
            TOTAL_SIMULATION_NANOS.get()
        );
    }

    public record Snapshot(
        long lastDay,
        int lastDayCellsAttempted,
        int lastDayCellsChanged,
        long totalBatches,
        long totalCellsAttempted,
        long totalCellsChanged,
        long lastBatchNanos,
        long slowestBatchNanos,
        long totalSimulationNanos
    ) {
        public double lastBatchMillis() { return lastBatchNanos / 1_000_000.0; }
        public double slowestBatchMillis() { return slowestBatchNanos / 1_000_000.0; }
        public double totalSimulationMillis() { return totalSimulationNanos / 1_000_000.0; }
    }
}
