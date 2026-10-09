package com.foundations.soil.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SavedTagChecks {
    private SavedTagChecks() {}
    public static ListTag compounds(CompoundTag root, String key, boolean required) {
        Tag value = root.get(key);
        if (value == null && !required) return new ListTag();
        if (!(value instanceof ListTag list) || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND))
            throw new IllegalStateException("Malformed or missing saved collection: " + key);
        return list;
    }
}
