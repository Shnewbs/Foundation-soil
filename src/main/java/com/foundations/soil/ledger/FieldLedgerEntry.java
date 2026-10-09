package com.foundations.soil.ledger;

import com.foundations.soil.config.SoilConfig;

import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilCellKey;
import com.foundations.soil.integration.TfcSoilSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Player-facing index entry for a discovered Soil Cell.
 *
 * This is deliberately a snapshot/reference, never the authoritative soil state.
 * SoilCell / SoilWorldData remain authoritative.
 */
public final class FieldLedgerEntry {
    public static final int MAX_RECENT_EVENTS = 12;

    private final String dimension;
    private final int cellX;
    private final int cellZ;
    private int sampleY;
    private long firstDiscoveredTick;
    private long lastScannedTick;
    private long lastScannedDay;
    private boolean favorite;
    private String note = "";
    private int fieldId;

    private com.foundations.soil.soil.SoilTexture texture;
    private final com.foundations.soil.soil.SoilTrend trend = new com.foundations.soil.soil.SoilTrend();
    private long lastProbeDay = -1;
    private double organicMatter;
    private double pH;
    private double drainage;
    private double compaction;
    private double structure;
    private double biology;
    private double salinity;
    private double nutrientAccess;
    private int amendmentApplications;
    private int cropCycles;
    private int continuousCropCycles;
    private double continuousPressure;
    private long daysBare;
    private long daysCovered;
    private double organicReserve;
    private int activePlantings;
    private String lastHarvestFamily = "";

    // Structured amendment status copied from the authoritative Soil Cell. The compact recentEvents
    // list remains the bounded player-facing history; these fields describe the latest/current state.
    private String lastAmendmentId = "";
    private long lastAmendmentDay = -1L;
    private double lastAmendmentAmount;
    private String lastAmendmentUnit = "";
    private double lastAmendmentConcentration = 1.0;
    private boolean lastAmendmentOverApplied;
    private int pendingAmendmentCount;
    private double pendingAmendmentDaysRemaining;
    private double recentAmendmentLoad;

    // Snapshot of the exact TFC soil/farmland sampled by the last Soil Probe scan.
    // These values are informational only; TFC remains authoritative.
    private String tfcSoilBlock = "";
    private boolean tfcFarmlandDataPresent;
    private int tfcHydration = -1;
    private int tfcMinHydration = -1;
    private int tfcMaxHydration = -1;
    private double tfcNitrogen;
    private double tfcPhosphorus;
    private double tfcPotassium;

    private final List<String> recentEvents = new ArrayList<>();

    public FieldLedgerEntry(String dimension, int cellX, int cellZ, int sampleY, long discoveredTick) {
        this.dimension = dimension;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.sampleY = sampleY;
        this.firstDiscoveredTick = Math.max(0, discoveredTick);
        this.lastScannedTick = Math.max(0, discoveredTick);
    }

    public java.util.Optional<com.foundations.soil.soil.SoilTexture> texture() { return java.util.Optional.ofNullable(texture); }
    public List<com.foundations.soil.soil.SoilTrend.Point> trendPoints() { return trend.points(); }
    public long lastProbeDay() { return lastProbeDay; }
    public com.foundations.soil.soil.SoilProfile profileSnapshot() {
        return new com.foundations.soil.soil.SoilProfile(organicMatter, pH, drainage, compaction, structure, biology, salinity);
    }
    public String dimension() { return dimension; }
    public int cellX() { return cellX; }
    public int cellZ() { return cellZ; }
    public int sampleY() { return sampleY; }
    public long firstDiscoveredTick() { return firstDiscoveredTick; }
    public long lastScannedTick() { return lastScannedTick; }
    public long lastScannedDay() { return lastScannedDay; }
    public boolean favorite() { return favorite; }
    public String note() { return note; }
    public int fieldId() { return fieldId; }
    public double organicMatter() { return organicMatter; }
    public double pH() { return pH; }
    public double drainage() { return drainage; }
    public double compaction() { return compaction; }
    public double structure() { return structure; }
    public double biology() { return biology; }
    public double salinity() { return salinity; }
    public double nutrientAccess() { return nutrientAccess; }
    public int amendmentApplications() { return amendmentApplications; }
    public int cropCycles() { return cropCycles; }
    public int continuousCropCycles() { return continuousCropCycles; }
    public double continuousPressure() { return continuousPressure; }
    public long daysBare() { return daysBare; }
    public long daysCovered() { return daysCovered; }
    public double organicReserve() { return organicReserve; }
    public int activePlantings() { return activePlantings; }
    public String lastHarvestFamily() { return lastHarvestFamily; }
    public String lastAmendmentId() { return lastAmendmentId; }
    public long lastAmendmentDay() { return lastAmendmentDay; }
    public double lastAmendmentAmount() { return lastAmendmentAmount; }
    public String lastAmendmentUnit() { return lastAmendmentUnit; }
    public double lastAmendmentConcentration() { return lastAmendmentConcentration; }
    public boolean lastAmendmentOverApplied() { return lastAmendmentOverApplied; }
    public int pendingAmendmentCount() { return pendingAmendmentCount; }
    public double pendingAmendmentDaysRemaining() { return pendingAmendmentDaysRemaining; }
    public double recentAmendmentLoad() { return recentAmendmentLoad; }
    public String tfcSoilBlock() { return tfcSoilBlock; }
    public boolean tfcFarmlandDataPresent() { return tfcFarmlandDataPresent; }
    public int tfcHydration() { return tfcHydration; }
    public int tfcMinHydration() { return tfcMinHydration; }
    public int tfcMaxHydration() { return tfcMaxHydration; }
    public double tfcNitrogen() { return tfcNitrogen; }
    public double tfcPhosphorus() { return tfcPhosphorus; }
    public double tfcPotassium() { return tfcPotassium; }
    public List<String> recentEvents() { return Collections.unmodifiableList(recentEvents); }

    public int centerX() { return cellX * SoilCellKey.CELL_SIZE + SoilCellKey.CELL_SIZE / 2; }
    public int centerZ() { return cellZ * SoilCellKey.CELL_SIZE + SoilCellKey.CELL_SIZE / 2; }
    public int chunkX() { return Math.floorDiv(centerX(), 16); }
    public int chunkZ() { return Math.floorDiv(centerZ(), 16); }

    public void setFavorite(boolean value) { favorite = value; }
    public void setNote(String value) { note = clean(value, 256); }
    public void setFieldId(int value) { fieldId = Math.max(0, value); }

    public void refresh(SoilCell cell, long tfcTick, long tfcDay) {
        sampleY = cell.sampleY();
        texture = cell.physicalInitialized() ? cell.texture() : null;
        trend.clear();
        for (var point : cell.trend().points()) trend.record(point.day(), point.profile(), point.cropPressure());
        lastScannedTick = Math.max(0, tfcTick);
        lastScannedDay = Math.max(0, tfcDay);
        var p = cell.profile();
        organicMatter = p.organicMatter();
        pH = p.pH();
        drainage = p.drainage();
        compaction = p.compaction();
        structure = p.structure();
        biology = p.biology();
        salinity = p.salinity();
        nutrientAccess = cell.nutrientAccessModifier();
        amendmentApplications = cell.amendmentApplications();
        cropCycles = cell.cropCycles();
        continuousCropCycles = cell.continuousCropCycles();
        continuousPressure = cell.continuousPressure();
        daysBare = cell.daysBare();
        daysCovered = cell.daysCovered();
        organicReserve = cell.organicReserve() + cell.residueReserve();
        activePlantings = cell.activePlantings();
        lastHarvestFamily = clean(cell.lastHarvestFamily(), 64);

        String previousAmendmentId = lastAmendmentId;
        long previousAmendmentDay = lastAmendmentDay;
        lastAmendmentId = clean(cell.lastAmendmentId(), 64);
        lastAmendmentDay = cell.lastAmendmentDay();
        lastAmendmentAmount = nonNegative(cell.lastAmendmentAmount());
        lastAmendmentUnit = clean(cell.lastAmendmentUnit(), 32);
        lastAmendmentConcentration = positiveOrOne(cell.lastAmendmentConcentration());
        pendingAmendmentCount = Math.max(0, cell.pendingAmendmentCount());
        pendingAmendmentDaysRemaining = cell.pendingAmendments().stream()
            .mapToDouble(effect -> Math.max(0.0, effect.remainingDays()))
            .max().orElse(0.0);
        recentAmendmentLoad = nonNegative(cell.recentAmendmentLoad());

        // The Soil Cell does not persist the historical overapplication boolean. Preserve it while
        // refreshing the same latest application, but clear it when the latest application changes.
        if (!lastAmendmentId.equals(previousAmendmentId) || lastAmendmentDay != previousAmendmentDay) {
            lastAmendmentOverApplied = false;
        }
    }

    /** Records the exact overapplication outcome from the authoritative application event. */
    void setLastAmendmentOverApplied(boolean value) { lastAmendmentOverApplied = value; }

    /** Captures a read-only TerraFirmaCraft snapshot from the exact block probed by the player. */
    public void refreshTfc(ServerLevel level, BlockPos soilPos) {
        TfcSoilSnapshot snapshot = TfcSoilSnapshot.capture(level, soilPos);
        lastProbeDay = Math.max(0, com.foundations.soil.integration.TfcRuntime.days());
        tfcSoilBlock = snapshot.soilBlock();
        tfcFarmlandDataPresent = snapshot.farmlandDataPresent();
        tfcHydration = snapshot.hydration();
        tfcMinHydration = snapshot.minHydration();
        tfcMaxHydration = snapshot.maxHydration();
        tfcNitrogen = snapshot.nitrogen();
        tfcPhosphorus = snapshot.phosphorus();
        tfcPotassium = snapshot.potassium();
    }

    public void addEvent(long tfcDay, String event) {
        String line = "Day " + Math.max(0, tfcDay) + ": " + clean(event, 120);
        recentEvents.add(0, line);
        while (recentEvents.size() > SoilConfig.LEDGER_MAX_RECENT_EVENTS.get()) recentEvents.remove(recentEvents.size() - 1);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("last_probe_day", lastProbeDay);
        if (texture != null) {
            CompoundTag physical = new CompoundTag();
            physical.putDouble("sand", texture.sand()); physical.putDouble("silt", texture.silt()); physical.putDouble("clay", texture.clay());
            tag.put("physical", physical);
        }
        tag.put("trend", trend.save());
        tag.putString("dimension", dimension);
        tag.putInt("cell_x", cellX);
        tag.putInt("cell_z", cellZ);
        tag.putInt("sample_y", sampleY);
        tag.putLong("first_discovered_tick", firstDiscoveredTick);
        tag.putLong("last_scanned_tick", lastScannedTick);
        tag.putLong("last_scanned_day", lastScannedDay);
        tag.putBoolean("favorite", favorite);
        tag.putString("note", note);
        tag.putInt("field_id", fieldId);
        tag.putDouble("organic_matter", organicMatter);
        tag.putDouble("ph", pH);
        tag.putDouble("drainage", drainage);
        tag.putDouble("compaction", compaction);
        tag.putDouble("structure", structure);
        tag.putDouble("biology", biology);
        tag.putDouble("salinity", salinity);
        tag.putDouble("nutrient_access", nutrientAccess);
        tag.putInt("amendment_applications", amendmentApplications);
        tag.putInt("crop_cycles", cropCycles);
        tag.putInt("continuous_crop_cycles", continuousCropCycles);
        tag.putDouble("continuous_pressure", continuousPressure);
        tag.putLong("days_bare", daysBare);
        tag.putLong("days_covered", daysCovered);
        tag.putDouble("organic_reserve", organicReserve);
        tag.putInt("active_plantings", activePlantings);
        tag.putString("last_harvest_family", lastHarvestFamily);
        tag.putString("last_amendment_id", lastAmendmentId);
        tag.putLong("last_amendment_day", lastAmendmentDay);
        tag.putDouble("last_amendment_amount", lastAmendmentAmount);
        tag.putString("last_amendment_unit", lastAmendmentUnit);
        tag.putDouble("last_amendment_concentration", lastAmendmentConcentration);
        tag.putBoolean("last_amendment_overapplied", lastAmendmentOverApplied);
        tag.putInt("pending_amendment_count", pendingAmendmentCount);
        tag.putDouble("pending_amendment_days_remaining", pendingAmendmentDaysRemaining);
        tag.putDouble("recent_amendment_load", recentAmendmentLoad);
        tag.putString("tfc_soil_block", tfcSoilBlock);
        tag.putBoolean("tfc_farmland_data_present", tfcFarmlandDataPresent);
        tag.putInt("tfc_hydration", tfcHydration);
        tag.putInt("tfc_min_hydration", tfcMinHydration);
        tag.putInt("tfc_max_hydration", tfcMaxHydration);
        tag.putDouble("tfc_nitrogen", tfcNitrogen);
        tag.putDouble("tfc_phosphorus", tfcPhosphorus);
        tag.putDouble("tfc_potassium", tfcPotassium);
        ListTag events = new ListTag();
        for (String event : recentEvents) events.add(StringTag.valueOf(event));
        tag.put("recent_events", events);
        return tag;
    }

    public static FieldLedgerEntry load(CompoundTag tag) {
        FieldLedgerEntry entry = new FieldLedgerEntry(
            tag.getString("dimension").orElse(""), tag.getInt("cell_x").orElse(0), tag.getInt("cell_z").orElse(0), tag.getInt("sample_y").orElse(0),
            tag.getLong("first_discovered_tick").orElse(0L)
        );
        entry.lastProbeDay = com.foundations.soil.storage.NbtAccess.contains(tag, "last_probe_day", Tag.TAG_LONG) ? tag.getLong("last_probe_day").orElse(0L) : -1;
        if (com.foundations.soil.storage.NbtAccess.contains(tag, "physical", Tag.TAG_COMPOUND)) {
            CompoundTag physical = tag.getCompound("physical").orElseThrow(() -> new IllegalStateException("Expected saved compound"));
            entry.texture = new com.foundations.soil.soil.SoilTexture(physical.getDouble("sand").orElse(0.0), physical.getDouble("silt").orElse(0.0), physical.getDouble("clay").orElse(0.0));
        }
        entry.trend.load(com.foundations.soil.storage.SavedTagChecks.compounds(tag, "trend", false));
        entry.lastScannedTick = Math.max(0, tag.getLong("last_scanned_tick").orElse(0L));
        entry.lastScannedDay = Math.max(0, tag.getLong("last_scanned_day").orElse(0L));
        entry.favorite = tag.getBoolean("favorite").orElse(false);
        entry.note = clean(tag.getString("note").orElse(""), 256);
        entry.fieldId = Math.max(0, tag.getInt("field_id").orElse(0));
        entry.organicMatter = tag.getDouble("organic_matter").orElse(0.0);
        entry.pH = tag.getDouble("ph").orElse(0.0);
        entry.drainage = tag.getDouble("drainage").orElse(0.0);
        entry.compaction = tag.getDouble("compaction").orElse(0.0);
        entry.structure = tag.getDouble("structure").orElse(0.0);
        entry.biology = tag.getDouble("biology").orElse(0.0);
        entry.salinity = tag.getDouble("salinity").orElse(0.0);
        entry.nutrientAccess = tag.getDouble("nutrient_access").orElse(0.0);
        for (double value : new double[]{entry.organicMatter, entry.pH, entry.drainage, entry.compaction,
                entry.structure, entry.biology, entry.salinity, entry.nutrientAccess})
            if (!Double.isFinite(value)) throw new IllegalStateException("Non-finite saved ledger measurement");
        entry.amendmentApplications = Math.max(0, tag.getInt("amendment_applications").orElse(0));
        entry.cropCycles = Math.max(0, tag.getInt("crop_cycles").orElse(0));
        entry.continuousCropCycles = Math.max(0, tag.getInt("continuous_crop_cycles").orElse(0));
        entry.continuousPressure = unit(tag.getDouble("continuous_pressure").orElse(0.0));
        entry.daysBare = Math.max(0, tag.getLong("days_bare").orElse(0L));
        entry.daysCovered = Math.max(0, tag.getLong("days_covered").orElse(0L));
        entry.organicReserve = unit(tag.getDouble("organic_reserve").orElse(0.0));
        entry.activePlantings = Math.max(0, tag.getInt("active_plantings").orElse(0));
        entry.lastHarvestFamily = clean(tag.getString("last_harvest_family").orElse(""), 64);
        entry.lastAmendmentId = clean(tag.getString("last_amendment_id").orElse(""), 64);
        entry.lastAmendmentDay = com.foundations.soil.storage.NbtAccess.contains(tag, "last_amendment_day", Tag.TAG_LONG) ? tag.getLong("last_amendment_day").orElse(0L) : -1L;
        entry.lastAmendmentAmount = nonNegative(tag.getDouble("last_amendment_amount").orElse(0.0));
        entry.lastAmendmentUnit = clean(tag.getString("last_amendment_unit").orElse(""), 32);
        entry.lastAmendmentConcentration = com.foundations.soil.storage.NbtAccess.contains(tag, "last_amendment_concentration", Tag.TAG_DOUBLE)
            ? positiveOrOne(tag.getDouble("last_amendment_concentration").orElse(0.0)) : 1.0;
        entry.lastAmendmentOverApplied = tag.getBoolean("last_amendment_overapplied").orElse(false);
        entry.pendingAmendmentCount = Math.max(0, tag.getInt("pending_amendment_count").orElse(0));
        entry.pendingAmendmentDaysRemaining = nonNegative(tag.getDouble("pending_amendment_days_remaining").orElse(0.0));
        entry.recentAmendmentLoad = nonNegative(tag.getDouble("recent_amendment_load").orElse(0.0));
        entry.tfcSoilBlock = clean(tag.getString("tfc_soil_block").orElse(""), 128);
        entry.tfcFarmlandDataPresent = tag.getBoolean("tfc_farmland_data_present").orElse(false);
        entry.tfcHydration = entry.tfcFarmlandDataPresent ? hydration(tag.getInt("tfc_hydration").orElse(0)) : -1;
        entry.tfcMinHydration = entry.tfcFarmlandDataPresent && com.foundations.soil.storage.NbtAccess.contains(tag, "tfc_min_hydration", Tag.TAG_INT) ? hydration(tag.getInt("tfc_min_hydration").orElse(0)) : -1;
        entry.tfcMaxHydration = entry.tfcFarmlandDataPresent && com.foundations.soil.storage.NbtAccess.contains(tag, "tfc_max_hydration", Tag.TAG_INT) ? hydration(tag.getInt("tfc_max_hydration").orElse(0)) : -1;
        entry.tfcNitrogen = unit(tag.getDouble("tfc_nitrogen").orElse(0.0));
        entry.tfcPhosphorus = unit(tag.getDouble("tfc_phosphorus").orElse(0.0));
        entry.tfcPotassium = unit(tag.getDouble("tfc_potassium").orElse(0.0));
        if (com.foundations.soil.storage.NbtAccess.contains(tag, "recent_events", Tag.TAG_LIST)) {
            ListTag events = com.foundations.soil.storage.NbtAccess.strings(tag, "recent_events");
            for (int i = 0; i < Math.min(SoilConfig.LEDGER_MAX_RECENT_EVENTS.get(), events.size()); i++) {
                entry.recentEvents.add(clean(events.getString(i).orElseThrow(), 160));
            }
        }
        return entry;
    }

    public static String key(String dimension, int cellX, int cellZ) {
        return dimension + "|" + cellX + "|" + cellZ;
    }

    public String key() { return key(dimension, cellX, cellZ); }

    private static String clean(String value, int max) { return LedgerText.clean(value, max); }

    private static int hydration(int value) { return Math.max(0, Math.min(100, value)); }
    private static double unit(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite saved ledger value");
        return Math.max(0.0, Math.min(1.0, value));
    }
    private static double nonNegative(double value) { return Double.isFinite(value) ? Math.max(0.0, value) : 0.0; }
    private static double positiveOrOne(double value) { return Double.isFinite(value) && value > 0.0 ? value : 1.0; }
}
