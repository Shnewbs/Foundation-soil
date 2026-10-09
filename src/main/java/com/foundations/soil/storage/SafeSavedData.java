package com.foundations.soil.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Minecraft catches deserializer exceptions. Never let its fallback replace an existing file. */
public final class SafeSavedData {
    private SafeSavedData() {}
    public static <T extends SavedData> T get(ServerLevel level, String name, Supplier<T> create,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Saved data requires the server thread");
        Path file = DimensionType.getStorageFolder(level.dimension(), level.getServer().getWorldPath(LevelResource.ROOT))
            .resolve("data").resolve(name + ".dat");
        return level.getDataStorage().computeIfAbsent(factory(file, create, load), name);
    }
    public static <T extends SavedData> SavedData.Factory<T> factory(Path file, Supplier<T> create,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        return new SavedData.Factory<>(() -> {
            // notExists is false for an inaccessible file too: fail closed in that case.
            if (!Files.notExists(file)) throw new IllegalStateException(
                "Refusing to replace unreadable Foundations saved data: " + file
                + ". Stop the server and restore a backup or repair the original; no empty replacement was created.");
            return create.get();
        }, load);
    }
}
