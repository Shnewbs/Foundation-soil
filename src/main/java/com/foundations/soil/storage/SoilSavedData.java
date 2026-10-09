package com.foundations.soil.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Keeps the proven compact Soil NBT format behind 26.1.2's codec-based SavedDataType. */
public abstract class SoilSavedData extends SavedData {
    public abstract CompoundTag save(CompoundTag root, HolderLookup.Provider registries);
}
