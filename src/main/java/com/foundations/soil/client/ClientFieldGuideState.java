package com.foundations.soil.client;

import com.foundations.soil.network.FieldGuideDataPayload;

/** Common-safe handoff so common payload registration never references Minecraft client classes. */
public final class ClientFieldGuideState {
    private static FieldGuideDataPayload pending;
    private static boolean refresh;
    public static boolean isRefresh() { return refresh; }
    public static void acceptRefresh(FieldGuideDataPayload payload) { pending = payload; refresh = true; }

    private ClientFieldGuideState() {}

    public static void accept(FieldGuideDataPayload payload) {
        pending = payload;
        refresh = false;
    }

    public static FieldGuideDataPayload take() {
        FieldGuideDataPayload payload = pending;
        pending = null;
        refresh = false;
        return payload;
    }

    public static void clear() {
        pending = null;
        refresh = false;
    }
}
