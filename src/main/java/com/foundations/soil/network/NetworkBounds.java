package com.foundations.soil.network;

/** Reject malformed lengths; silently truncating a count leaves the rest of a packet misaligned. */
public final class NetworkBounds {
    private NetworkBounds() {}
    public static int count(int count, int maximum) {
        if (count < 0 || count > maximum) throw new IllegalArgumentException("Invalid payload count: " + count);
        return count;
    }
    public static int pageOffset(int requested, int size, int pageSize) {
        if (requested < 0) throw new IllegalArgumentException("Negative page offset");
        int lastPage = size == 0 ? 0 : ((size - 1) / pageSize) * pageSize;
        return Math.min(lastPage, (requested / pageSize) * pageSize);
    }
}
