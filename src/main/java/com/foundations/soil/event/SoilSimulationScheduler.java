package com.foundations.soil.event;

import com.foundations.soil.diagnostic.SoilRuntimeMetrics;
import com.foundations.soil.soil.SoilManager;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;

final class SoilSimulationScheduler {
    private static final Map<ServerLevel, State> STATES = new IdentityHashMap<>();

    private SoilSimulationScheduler() {}

    static void tick(ServerLevel level, long day, int budget) {
        State state = STATES.get(level);
        if (state == null || (day > state.day && state.index >= state.keys.length)) {
            state = new State(day, SoilManager.packedCellKeys(level), 0);
            STATES.put(level, state);
        }

        int left = Math.max(1, budget);
        int attempted = 0;
        int changed = 0;
        long started = System.nanoTime();
        while (left-- > 0 && state.index < state.keys.length) {
            attempted++;
            if (SoilManager.simulatePacked(level, state.keys[state.index++], state.day)) changed++;
        }
        if (attempted > 0) {
            SoilRuntimeMetrics.recordBatch(state.day, attempted, changed, System.nanoTime() - started);
        }
    }

    static void clear() { STATES.clear(); }

    private static final class State {
        final long day;
        final long[] keys;
        int index;
        State(long day, long[] keys, int index) { this.day = day; this.keys = keys; this.index = index; }
    }
}
