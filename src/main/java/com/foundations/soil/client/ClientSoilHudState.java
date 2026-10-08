package com.foundations.soil.client;

import com.foundations.soil.network.SoilHudSnapshotPayload;

/**
 * Common-safe client cache. This class intentionally imports no Minecraft client classes so
 * the payload registration can reference it without dedicated-server classloading hazards.
 */
public final class ClientSoilHudState {
    private static long requestedPos = Long.MIN_VALUE;
    private static SoilHudSnapshotPayload snapshot;

    private ClientSoilHudState() {}

    public static void target(long packedPos) {
        if (requestedPos != packedPos) {
            requestedPos = packedPos;
            snapshot = null;
        }
    }

    public static long requestedPos() {
        return requestedPos;
    }

    public static SoilHudSnapshotPayload snapshot() {
        return snapshot != null && snapshot.packedPos() == requestedPos ? snapshot : null;
    }

    public static void accept(SoilHudSnapshotPayload payload) {
        if (payload.packedPos() == requestedPos) snapshot = payload;
    }

    public static void clear() {
        requestedPos = Long.MIN_VALUE;
        snapshot = null;
    }
}
