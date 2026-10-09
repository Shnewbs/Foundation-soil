package com.foundations.soil.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SavedTagChecks {
    private SavedTagChecks() {}
    /** Missing headers are legacy; a present header must be a supported positive integer. */
    public static int version(CompoundTag tag, String key, int legacy, int supported) {
        if (!tag.contains(key)) return legacy;
        if (!NbtAccess.contains(tag, key, Tag.TAG_INT)) throw new IllegalStateException("Malformed saved version: " + key);
        int value = tag.getInt(key).orElseThrow();
        if (value < 1 || value > supported) throw new IllegalStateException("Unsupported saved " + key + ": " + value);
        return value;
    }
    /** Optional fields may be absent in older saves, but never silently discarded when ill-typed. */
    public static void optionalType(CompoundTag tag, String key, int expected) {
        if (tag.contains(key) && !NbtAccess.contains(tag, key, expected))
            throw new IllegalStateException("Malformed saved value: " + key);
    }
    public static int requiredInt(CompoundTag tag, String key) {
        if (!NbtAccess.contains(tag, key, Tag.TAG_INT)) throw new IllegalStateException("Missing/malformed saved integer: " + key);
        return tag.getInt(key).orElseThrow();
    }
    public static double requiredDouble(CompoundTag tag, String key) {
        if (!NbtAccess.contains(tag, key, Tag.TAG_DOUBLE)) throw new IllegalStateException("Missing/malformed saved measurement: " + key);
        double value = tag.getDouble(key).orElseThrow();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite saved measurement: " + key);
        return value;
    }
    public static ListTag compounds(CompoundTag root, String key, boolean required) {
        Tag value = root.get(key);
        if (value == null && !required) return new ListTag();
        if (!(value instanceof ListTag list) || list.stream().anyMatch(entry -> entry.getId() != Tag.TAG_COMPOUND))
            throw new IllegalStateException("Malformed or missing saved collection: " + key);
        return list;
    }
}
