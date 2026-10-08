package com.foundations.soil.soil;

import com.foundations.soil.FoundationsSoil;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public final class SoilWorldData extends SavedData {
    public static final String DATA_NAME = "foundations_soil_cells";
    private final Map<Long, SoilCell> cells = new HashMap<>();

    public Optional<SoilCell> get(SoilCellKey key) { return Optional.ofNullable(cells.get(key.packed())); }
    public Optional<SoilCell> getPacked(long packed) { return Optional.ofNullable(cells.get(packed)); }
    public SoilCell put(SoilCellKey key, SoilCell cell) { cells.put(key.packed(), cell); setDirty(); return cell; }
    public Collection<SoilCell> cells() { return Collections.unmodifiableCollection(cells.values()); }
    public int size() { return cells.size(); }
    public long[] packedKeysSnapshot() { return cells.keySet().stream().mapToLong(Long::longValue).toArray(); }

    public static SoilWorldData load(CompoundTag root, HolderLookup.Provider registries) {
        int schema = root.contains("schema", Tag.TAG_INT) ? root.getInt("schema") : 1;
        int model = root.contains("model", Tag.TAG_INT) ? root.getInt("model") : 1;
        if (schema > SoilCell.SCHEMA_VERSION) throw new IllegalStateException("Foundations Soil save schema " + schema + " is newer than supported schema " + SoilCell.SCHEMA_VERSION);
        if (model > SoilCell.MODEL_VERSION) throw new IllegalStateException("Foundations Soil model " + model + " is newer than supported model " + SoilCell.MODEL_VERSION);

        SoilWorldData data = new SoilWorldData();
        ListTag list = root.getList("cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            SoilCell cell = SoilCell.load(list.getCompound(i));
            data.cells.put(new SoilCellKey(cell.cellX(), cell.cellZ()).packed(), cell);
        }
        if (schema < SoilCell.SCHEMA_VERSION || model < SoilCell.MODEL_VERSION) data.setDirty();
        FoundationsSoil.LOGGER.info("[Foundations Soil] Loaded {} persistent soil cells (save schema={} -> {}, model={} -> {})",
            data.cells.size(), schema, SoilCell.SCHEMA_VERSION, model, SoilCell.MODEL_VERSION);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        root.putInt("schema", SoilCell.SCHEMA_VERSION);
        root.putInt("model", SoilCell.MODEL_VERSION);
        ListTag list = new ListTag();
        for (SoilCell cell : cells.values()) list.add(cell.save());
        root.put("cells", list);
        return root;
    }
}
