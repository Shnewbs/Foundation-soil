package com.foundations.soil.client;

import com.foundations.soil.network.LedgerEditReply;
/** Common-safe packet hand-off; Minecraft client classes stay outside common network initialization. */
public final class ClientLedgerEditState {
    private static LedgerEditReply pending;
    private ClientLedgerEditState() {}
    public static void accept(LedgerEditReply value) { pending = value; }
    public static LedgerEditReply take() { LedgerEditReply value = pending; pending = null; return value; }
    public static void clear() { pending = null; }
}
