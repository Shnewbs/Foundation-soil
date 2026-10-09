package com.foundations.soil.network;

import java.util.ArrayDeque;
import java.util.UUID;

/** Per-connection, bounded replay/cooldown guard. Clock is the server tick, not client-supplied time. */
public final class RequestWindow {
    private long nextEdit = Long.MIN_VALUE, nextRead = Long.MIN_VALUE;
    private long nextHud = Long.MIN_VALUE;
    private final ArrayDeque<UUID> recent = new ArrayDeque<>(32);
    public boolean edit(UUID id, long tick) {
        if (id == null || tick < nextEdit || recent.contains(id)) return false;
        nextEdit = tick + 10;
        if (recent.size() == 32) recent.removeFirst();
        recent.addLast(id); return true;
    }
    public boolean hud(long tick) {
        if (tick < nextHud) return false;
        nextHud = tick + 4; return true;
    }
    public boolean read(long tick) {
        if (tick < nextRead) return false;
        nextRead = tick + 10; return true;
    }
}
