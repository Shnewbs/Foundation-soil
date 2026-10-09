package com.foundations.soil.storage;

import com.foundations.soil.FoundationsSoil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;

/** Namespaced 26.1.2 saved data, with fail-closed loading and explicit legacy-file import. */
public final class SafeSavedData {
    private SafeSavedData() {}
    public static <T extends SoilSavedData> T get(ServerLevel level, String name, Supplier<T> create,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Saved data requires the server thread");
        Path world = level.getServer().getWorldPath(LevelResource.ROOT);
        Path folder = DimensionType.getStorageFolder(level.dimension(), world).resolve("data");
        Identifier id = Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, name);
        Path file = id.withSuffix(".dat").resolveAgainst(folder);
        // Vanilla's world conversion can leave old addon data under either legacy spelling.
        List<Path> legacy = List.of(folder.resolve(name + ".dat"), folder.resolve("minecraft").resolve(name + ".dat"));
        return level.getDataStorage().computeIfAbsent(factory(file, id, () -> importLegacy(legacy, create, load), load));
    }
    public static <T extends SoilSavedData> SavedDataType<T> factory(Path file, Identifier id, Supplier<T> create,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        Codec<T> codec = CompoundTag.CODEC.flatXmap(tag -> {
            try { return DataResult.success(load.apply(tag, null)); }
            catch (RuntimeException failure) { return DataResult.error(() -> "Invalid Foundations saved data: " + failure.getMessage()); }
        }, value -> {
            try { return DataResult.success(value.save(new CompoundTag(), null)); }
            catch (RuntimeException failure) { return DataResult.error(() -> "Cannot encode Foundations saved data: " + failure.getMessage()); }
        });
        return new SavedDataType<>(id, () -> {
            if (!Files.notExists(file)) throw new IllegalStateException("Refusing to replace unreadable Foundations saved data: "
                + file + ". Restore or repair the original; no empty replacement was created.");
            return create.get();
        }, codec);
    }
    public static <T extends SoilSavedData> T importLegacy(List<Path> candidates, Supplier<T> create,
            BiFunction<CompoundTag, HolderLookup.Provider, T> load) {
        List<Path> found = candidates.stream().filter(path -> !Files.notExists(path)).distinct().toList();
        if (found.isEmpty()) return create.get();
        if (found.size() != 1) throw new IllegalStateException("Ambiguous legacy Soil files: " + found + "; refusing to guess or discard one.");
        Path original = found.getFirst();
        try {
            CompoundTag wrapper = NbtIo.readCompressed(original, NbtAccounter.create(256L * 1024 * 1024));
            CompoundTag data = wrapper.getCompound("data").orElseThrow(() -> new IllegalStateException("Legacy saved data lacks its data compound"));
            T migrated = load.apply(data, null);
            migrated.setDirty();
            FoundationsSoil.LOGGER.info("[Foundations Soil] Imported legacy data from {}; original retained", original);
            return migrated;
        } catch (IOException | RuntimeException failure) {
            throw new IllegalStateException("Cannot migrate legacy Soil data " + original + "; original was not modified", failure);
        }
    }
}
