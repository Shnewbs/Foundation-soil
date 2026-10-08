package com.foundations.soil.client;

import com.foundations.soil.network.FieldGuideDataPayload;

/** Common-safe handoff so common payload registration never references Minecraft client classes. */
public final class ClientFieldGuideState {
    private static FieldGuideDataPayload pending;

    private ClientFieldGuideState() {}

    public static void accept(FieldGuideDataPayload payload) {
        pending = payload;
    }

    public static FieldGuideDataPayload take() {
        FieldGuideDataPayload payload = pending;
        pending = null;
        return payload;
    }

    public static void clear() {
        pending = null;
    }
}
