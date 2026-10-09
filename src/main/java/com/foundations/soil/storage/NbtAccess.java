package com.foundations.soil.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Type checks retained across the Optional-based NBT API transition. */
public final class NbtAccess {
    private NbtAccess() {}
    public static boolean contains(CompoundTag tag, String key, int expected) {
        Tag value = tag.get(key);
        return value != null && value.getId() == expected;
    }
    public static ListTag strings(CompoundTag tag, String key) {
        Tag value = tag.get(key);
        if (value == null) return new ListTag();
        if (!(value instanceof ListTag list)) throw new IllegalStateException("Expected saved list: " + key);
        for (Tag entry : list) if (entry.getId() != Tag.TAG_STRING) throw new IllegalStateException("Expected saved string: " + key);
        return list;
    }
}
