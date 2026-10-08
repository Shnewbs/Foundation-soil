package com.foundations.soil.soil;

import com.foundations.soil.amendment.AmendmentApplicationOutcome;
import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.AmendmentEffect;
import com.foundations.soil.amendment.PendingAmendmentEffect;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.crop.CropFamilyDefinition;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.crop.CropFamily;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class SoilCell {
    public static final int SCHEMA_VERSION = 3;
    public static final int MODEL_VERSION = 3;

    private final int cellX;
    private final int cellZ;
    private int sampleY;
    private SoilProfile profile;
    private long lastTfcTick;
    private long lastSimulatedDay;
    private int amendmentApplications;

    private int cropCycles;
    private int continuousCropCycles;
    private long daysBare;
    private long daysCovered;
    private int organicInputs;
    private int disturbanceEvents;
    private int waterloggingEvents;
    private int compactionEvents;
    private int activePlantings;
    private String currentCropFamily = "";
    private String lastHarvestFamily = "";
    private double continuousPressure;
    private double organicReserve;
    private double organicReserveRate = 0.030;
    private double residueReserve;
    private double recentAmendmentLoad;
    private final ArrayList<PendingAmendmentEffect> pendingAmendments = new ArrayList<>();
    private String lastAmendmentId = "";
    private long lastAmendmentDay = -1L;
    private double lastAmendmentAmount;
    private String lastAmendmentUnit = "";
    private double lastAmendmentConcentration = 1.0;
    private boolean simulationBaselinePending;

    public SoilCell(int cellX, int cellZ, SoilProfile profile, long lastTfcTick) {
        this(cellX, cellZ, 64, profile, lastTfcTick, 0L);
    }

    public SoilCell(int cellX, int cellZ, int sampleY, SoilProfile profile, long lastTfcTick, long lastSimulatedDay) {
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.sampleY = sampleY;
        this.profile = profile;
        this.lastTfcTick = Math.max(0L, lastTfcTick);
        this.lastSimulatedDay = Math.max(0L, lastSimulatedDay);
    }

    public int cellX() { return cellX; }
    public int cellZ() { return cellZ; }
    public int sampleY() { return sampleY; }
    public SoilProfile profile() { return profile; }
    public long lastTfcTick() { return lastTfcTick; }
    public long lastSimulatedDay() { return lastSimulatedDay; }
    public int amendmentApplications() { return amendmentApplications; }
    public int cropCycles() { return cropCycles; }
    public int continuousCropCycles() { return continuousCropCycles; }
    public long daysBare() { return daysBare; }
    public long daysCovered() { return daysCovered; }
    public int organicInputs() { return organicInputs; }
    public int disturbanceEvents() { return disturbanceEvents; }
    public int waterloggingEvents() { return waterloggingEvents; }
    public int compactionEvents() { return compactionEvents; }
    public int activePlantings() { return activePlantings; }
    public String currentCropFamily() { return currentCropFamily; }
    public String lastHarvestFamily() { return lastHarvestFamily; }
    public double continuousPressure() { return continuousPressure; }
    public double organicReserve() { return organicReserve; }
    public double organicReserveRate() { return organicReserveRate; }
    public double residueReserve() { return residueReserve; }
    public double recentAmendmentLoad() { return recentAmendmentLoad; }
    public int pendingAmendmentCount() { return pendingAmendments.size(); }
    public List<PendingAmendmentEffect> pendingAmendments() { return List.copyOf(pendingAmendments); }
    public String lastAmendmentId() { return lastAmendmentId; }
    public long lastAmendmentDay() { return lastAmendmentDay; }
    public double lastAmendmentAmount() { return lastAmendmentAmount; }
    public String lastAmendmentUnit() { return lastAmendmentUnit; }
    public double lastAmendmentConcentration() { return lastAmendmentConcentration; }
    public boolean simulationBaselinePending() { return simulationBaselinePending; }

    /** Cell-specific effective access including summarized continuous-cropping pressure. */
    public double nutrientAccessModifier() {
        return Math.max(0.20, Math.min(1.10, profile.nutrientAvailabilityModifier() * (1.0 - continuousPressure * 0.18)));
    }

    public void touch(long tfcTick) { this.lastTfcTick = Math.max(lastTfcTick, tfcTick); }
    public void touch(long tfcTick, int y) { this.sampleY = y; touch(tfcTick); }

    /** Legacy one-reference-dose path retained for existing integrations/tests. */
    public double apply(AmendmentDefinition amendment, long tfcTick) {
        return apply(amendment, tfcTick, lastSimulatedDay, SoilConfig.AMENDMENT_DIMINISHING_STRENGTH.get());
    }

    /** Legacy one-reference-dose path retained for existing integrations/tests. */
    public double apply(AmendmentDefinition amendment, long tfcTick, long currentDay, double diminishingStrength) {
        AmendmentApplicationOutcome outcome = applyDose(amendment, amendment.referenceAmount(), amendment.doseUnit(), 1.0,
            tfcTick, currentDay, diminishingStrength, 1.0, 32);
        return outcome.effectiveness();
    }

    /** Authoritative 0.0.3a solid/liquid amendment application. */
    public AmendmentApplicationOutcome applyDose(AmendmentDefinition amendment, double amount, AmendmentDoseUnit unit,
                                                   double concentration, long tfcTick, long currentDay,
                                                   double diminishingStrength, double globalStrength, int maxPending) {
        if (amendment == null) throw new IllegalArgumentException("amendment cannot be null");
        if (unit == null) throw new IllegalArgumentException("amendment dose unit cannot be null");
        if (!Double.isFinite(globalStrength) || globalStrength < 0) throw new IllegalArgumentException("globalStrength must be finite and >= 0");
        double normalizedDose = amendment.normalizedDose(amount, unit, concentration);
        double effectiveness = 1.0 / (1.0 + Math.max(0.0, recentAmendmentLoad) * Math.max(0.0, diminishingStrength));
        double effectScale = normalizedDose * globalStrength * effectiveness;

        AmendmentEffect total = AmendmentEffect.from(amendment).scale(effectScale);
        double immediateFraction = amendment.releaseDays() > 0 ? amendment.immediateFraction() : 1.0;
        AmendmentEffect immediate = total.scale(immediateFraction);
        AmendmentEffect residual = total.scale(1.0 - immediateFraction);

        boolean overApplied = amendment.overapplicationThreshold() > 0
            && normalizedDose > amendment.overapplicationThreshold();
        double overSalinity = overApplied
            ? (normalizedDose - amendment.overapplicationThreshold()) * amendment.overapplicationSalinity() * globalStrength
            : 0.0;
        if (overSalinity > 0) immediate = immediate.plus(new AmendmentEffect(0, 0, 0, 0, 0, 0, overSalinity));
        applyDirectEffect(immediate);

        if (!residual.effectivelyZero() && amendment.releaseDays() > 0) {
            queuePending(new PendingAmendmentEffect(amendment.id(), amendment.releaseDays(), residual), Math.max(1, maxPending));
        }

        double reserveAddition = amendment.organicReserve() * effectScale;
        if (reserveAddition > 0) {
            double combined = organicReserve + reserveAddition;
            organicReserveRate = combined <= 0 ? organicReserveRate
                : ((organicReserve * organicReserveRate) + (reserveAddition * amendment.decompositionRate())) / combined;
            organicReserve = unit(combined);
        }

        recentAmendmentLoad = Math.max(0.0, recentAmendmentLoad + amendment.diminishingWeight() * normalizedDose);
        amendmentApplications++;
        if (amendment.organicMatter() > 0 || amendment.organicReserve() > 0) organicInputs++;
        lastAmendmentId = amendment.id();
        lastAmendmentDay = Math.max(0, currentDay);
        lastAmendmentAmount = amount;
        lastAmendmentUnit = unit.name();
        lastAmendmentConcentration = concentration;
        lastSimulatedDay = Math.max(lastSimulatedDay, currentDay);
        touch(tfcTick);

        return new AmendmentApplicationOutcome(amendment.id(), amendment.form(), amount, unit, concentration,
            normalizedDose, effectiveness, immediateFraction, 1.0 - immediateFraction, pendingAmendments.size(),
            overApplied, overSalinity);
    }

    /** Releases persisted delayed direct amendment effects for the elapsed TFC calendar interval. */
    int releasePendingAmendments(double elapsedDays) {
        if (!Double.isFinite(elapsedDays) || elapsedDays <= 0 || pendingAmendments.isEmpty()) return 0;
        AmendmentEffect released = new AmendmentEffect(0, 0, 0, 0, 0, 0, 0);
        ArrayList<PendingAmendmentEffect> next = new ArrayList<>(pendingAmendments.size());
        int completed = 0;
        for (PendingAmendmentEffect pending : pendingAmendments) {
            PendingAmendmentEffect.Release step = pending.release(elapsedDays);
            released = released.plus(step.released());
            if (step.remaining() != null) next.add(step.remaining());
            else completed++;
        }
        pendingAmendments.clear();
        pendingAmendments.addAll(next);
        applyDirectEffect(released);
        return completed;
    }

    private void applyDirectEffect(AmendmentEffect effect) {
        if (effect == null || effect.effectivelyZero()) return;
        profile = profile.adjust(effect.organicMatter(), effect.pH(), effect.drainage(), effect.compaction(),
            effect.structure(), effect.biology(), effect.salinity());
    }

    private void queuePending(PendingAmendmentEffect pending, int maxPending) {
        // Keep individual applications independent while capacity is available so a new dose never
        // extends an older dose's release schedule. Only merge as a bounded-persistence fallback.
        if (pendingAmendments.size() < maxPending) {
            pendingAmendments.add(pending);
            return;
        }
        for (int i = 0; i < pendingAmendments.size(); i++) {
            PendingAmendmentEffect existing = pendingAmendments.get(i);
            if (existing.amendmentId().equals(pending.amendmentId())) {
                pendingAmendments.set(i, existing.merge(pending));
                return;
            }
        }
        // Bounded persistence: when full, merge into the last residue instead of dropping effects.
        int last = pendingAmendments.size() - 1;
        pendingAmendments.set(last, pendingAmendments.get(last).merge(pending));
    }

    public void recordPlanting(CropFamily family, long tfcTick, long currentDay, int y) {
        CropFamily actual = family == null ? CropFamily.OTHER : family;
        activePlantings = Math.min(16, activePlantings + 1);
        currentCropFamily = actual.id();
        if (!lastHarvestFamily.isBlank() && !lastHarvestFamily.equals(actual.id())) {
            continuousPressure *= SoilConfig.PLANTING_ROTATION_RELIEF.get();
            continuousCropCycles = 0;
        }
        touch(tfcTick, y);
        lastSimulatedDay = Math.max(lastSimulatedDay, currentDay);
    }

    public void reconcilePlantings(int active, String family) {
        this.activePlantings = Math.max(0, Math.min(16, active));
        this.currentCropFamily = activePlantings == 0 ? "" : (family == null ? "" : family);
    }

    public void recordCropRemoval(CropFamily family, boolean mature, long tfcTick, long currentDay) {
        activePlantings = Math.max(0, activePlantings - 1);
        if (activePlantings == 0) currentCropFamily = "";
        if (mature) recordHarvest(family);
        else disturbanceEvents++;
        touch(tfcTick);
        lastSimulatedDay = Math.max(lastSimulatedDay, currentDay);
    }

    private void recordHarvest(CropFamily family) {
        CropFamily actual = family == null ? CropFamily.OTHER : family;
        cropCycles++;
        if (lastHarvestFamily.equals(actual.id())) {
            continuousCropCycles++;
        } else {
            continuousCropCycles = 1;
            continuousPressure *= SoilConfig.HARVEST_ROTATION_RELIEF.get();
        }
        lastHarvestFamily = actual.id();
        double cyclePressure = SoilConfig.CROP_PRESSURE_BASE.get()
            + Math.min(SoilConfig.CROP_PRESSURE_REPEAT_CAP.get(),
                Math.max(0, continuousCropCycles - 1) * SoilConfig.CROP_PRESSURE_REPEAT.get());
        continuousPressure = unit(continuousPressure + cyclePressure);

        CropFamilyDefinition familyDefinition = CropRegistry.familyDefinition(actual);
        residueReserve = unit(residueReserve + familyDefinition.residueReserve());
        profile = profile.adjust(
            familyDefinition.organicMatterDelta(), 0, 0,
            familyDefinition.compactionDelta(), familyDefinition.structureDelta(),
            familyDefinition.biologyDelta(), familyDefinition.salinityDelta()
        );
    }

    public void recordDisturbance(double compactionDelta, double structureDelta, long tfcTick, long currentDay, int y) {
        disturbanceEvents++;
        if (compactionDelta > 0) compactionEvents++;
        profile = profile.adjust(0, 0, 0, compactionDelta, structureDelta, -Math.abs(compactionDelta) * 0.10, 0);
        touch(tfcTick, y);
        lastSimulatedDay = Math.max(lastSimulatedDay, currentDay);
    }

    public void resetTo(SoilProfile newProfile, int y, long tfcTick, long currentDay) {
        profile = newProfile;
        sampleY = y;
        lastTfcTick = Math.max(0, tfcTick);
        lastSimulatedDay = Math.max(0, currentDay);
        amendmentApplications = 0;
        cropCycles = 0;
        continuousCropCycles = 0;
        daysBare = 0;
        daysCovered = 0;
        organicInputs = 0;
        disturbanceEvents = 0;
        waterloggingEvents = 0;
        compactionEvents = 0;
        activePlantings = 0;
        currentCropFamily = "";
        lastHarvestFamily = "";
        continuousPressure = 0;
        organicReserve = 0;
        organicReserveRate = 0.030;
        residueReserve = 0;
        recentAmendmentLoad = 0;
        pendingAmendments.clear();
        lastAmendmentId = "";
        lastAmendmentDay = -1;
        lastAmendmentAmount = 0;
        lastAmendmentUnit = "";
        lastAmendmentConcentration = 1.0;
        simulationBaselinePending = false;
    }

    void establishSimulationBaseline(long day, long tfcTick) {
        this.lastSimulatedDay = Math.max(0, day);
        this.simulationBaselinePending = false;
        touch(tfcTick);
    }

    void setSimulationState(SoilProfile profile, long day, long tfcTick, long bareDays, long coveredDays,
                            int waterloggingEvents, double pressure, double organicReserve,
                            double residueReserve, double recentAmendmentLoad) {
        this.profile = profile;
        this.lastSimulatedDay = Math.max(this.lastSimulatedDay, day);
        touch(tfcTick);
        this.daysBare = Math.max(0, bareDays);
        this.daysCovered = Math.max(0, coveredDays);
        this.waterloggingEvents = Math.max(0, waterloggingEvents);
        this.continuousPressure = unit(pressure);
        this.organicReserve = unit(organicReserve);
        this.residueReserve = unit(residueReserve);
        this.recentAmendmentLoad = Math.max(0, recentAmendmentLoad);
    }

    public void replaceProfile(SoilProfile newProfile, long tfcTick, long currentDay, int y) {
        this.profile = newProfile;
        this.sampleY = y;
        this.lastSimulatedDay = Math.max(lastSimulatedDay, currentDay);
        touch(tfcTick);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", SCHEMA_VERSION);
        tag.putInt("model", MODEL_VERSION);
        tag.putInt("cell_x", cellX);
        tag.putInt("cell_z", cellZ);
        tag.putInt("sample_y", sampleY);
        tag.putDouble("organic_matter", profile.organicMatter());
        tag.putDouble("ph", profile.pH());
        tag.putDouble("drainage", profile.drainage());
        tag.putDouble("compaction", profile.compaction());
        tag.putDouble("structure", profile.structure());
        tag.putDouble("biology", profile.biology());
        tag.putDouble("salinity", profile.salinity());
        tag.putLong("last_tfc_tick", lastTfcTick);
        tag.putLong("last_simulated_day", lastSimulatedDay);
        tag.putInt("amendment_applications", amendmentApplications);
        tag.putInt("crop_cycles", cropCycles);
        tag.putInt("continuous_crop_cycles", continuousCropCycles);
        tag.putLong("days_bare", daysBare);
        tag.putLong("days_covered", daysCovered);
        tag.putInt("organic_inputs", organicInputs);
        tag.putInt("disturbance_events", disturbanceEvents);
        tag.putInt("waterlogging_events", waterloggingEvents);
        tag.putInt("compaction_events", compactionEvents);
        tag.putInt("active_plantings", activePlantings);
        tag.putString("current_crop_family", currentCropFamily);
        tag.putString("last_harvest_family", lastHarvestFamily);
        tag.putDouble("continuous_pressure", continuousPressure);
        tag.putDouble("organic_reserve", organicReserve);
        tag.putDouble("organic_reserve_rate", organicReserveRate);
        tag.putDouble("residue_reserve", residueReserve);
        tag.putDouble("recent_amendment_load", recentAmendmentLoad);
        ListTag pending = new ListTag();
        for (PendingAmendmentEffect effect : pendingAmendments) pending.add(effect.save());
        tag.put("pending_amendments", pending);
        tag.putString("last_amendment_id", lastAmendmentId);
        tag.putLong("last_amendment_day", lastAmendmentDay);
        tag.putDouble("last_amendment_amount", lastAmendmentAmount);
        tag.putString("last_amendment_unit", lastAmendmentUnit);
        tag.putDouble("last_amendment_concentration", lastAmendmentConcentration);
        return tag;
    }

    public static SoilCell load(CompoundTag tag) {
        int schema = tag.contains("schema", Tag.TAG_INT) ? tag.getInt("schema") : 1;
        int model = tag.contains("model", Tag.TAG_INT) ? tag.getInt("model") : 1;
        if (schema > SCHEMA_VERSION) throw new IllegalStateException("Foundations Soil cell schema " + schema + " is newer than supported schema " + SCHEMA_VERSION);
        if (model > MODEL_VERSION) throw new IllegalStateException("Foundations Soil cell model " + model + " is newer than supported model " + MODEL_VERSION);

        SoilProfile profile = new SoilProfile(
            tag.getDouble("organic_matter"), tag.getDouble("ph"), tag.getDouble("drainage"),
            tag.getDouble("compaction"), tag.getDouble("structure"), tag.getDouble("biology"),
            tag.getDouble("salinity")
        );
        int sampleY = tag.contains("sample_y", Tag.TAG_INT) ? tag.getInt("sample_y") : 64;
        long simDay = tag.contains("last_simulated_day", Tag.TAG_LONG) ? tag.getLong("last_simulated_day") : 0L;
        SoilCell cell = new SoilCell(tag.getInt("cell_x"), tag.getInt("cell_z"), sampleY, profile, tag.getLong("last_tfc_tick"), simDay);
        cell.amendmentApplications = nonNegative(tag.getInt("amendment_applications"));
        cell.cropCycles = nonNegative(tag.getInt("crop_cycles"));
        cell.continuousCropCycles = nonNegative(tag.getInt("continuous_crop_cycles"));
        cell.daysBare = Math.max(0, tag.getLong("days_bare"));
        cell.daysCovered = Math.max(0, tag.getLong("days_covered"));
        cell.organicInputs = nonNegative(tag.getInt("organic_inputs"));
        cell.disturbanceEvents = nonNegative(tag.getInt("disturbance_events"));
        cell.waterloggingEvents = nonNegative(tag.getInt("waterlogging_events"));
        cell.compactionEvents = nonNegative(tag.getInt("compaction_events"));
        cell.activePlantings = nonNegative(tag.getInt("active_plantings"));
        cell.currentCropFamily = tag.getString("current_crop_family");
        cell.lastHarvestFamily = tag.getString("last_harvest_family");
        cell.continuousPressure = unit(tag.getDouble("continuous_pressure"));
        cell.organicReserve = unit(tag.getDouble("organic_reserve"));
        cell.organicReserveRate = tag.contains("organic_reserve_rate", Tag.TAG_DOUBLE) ? Math.max(0, tag.getDouble("organic_reserve_rate")) : 0.030;
        cell.residueReserve = unit(tag.getDouble("residue_reserve"));
        cell.recentAmendmentLoad = Math.max(0, tag.getDouble("recent_amendment_load"));
        if (tag.contains("pending_amendments", Tag.TAG_LIST)) {
            ListTag pending = tag.getList("pending_amendments", Tag.TAG_COMPOUND);
            for (int i = 0; i < pending.size(); i++) {
                try { cell.pendingAmendments.add(PendingAmendmentEffect.load(pending.getCompound(i))); }
                catch (RuntimeException ignored) { /* malformed residue is skipped rather than corrupting the whole cell */ }
            }
        }
        cell.lastAmendmentId = tag.getString("last_amendment_id");
        cell.lastAmendmentDay = tag.contains("last_amendment_day", Tag.TAG_LONG) ? tag.getLong("last_amendment_day") : -1L;
        cell.lastAmendmentAmount = tag.getDouble("last_amendment_amount");
        cell.lastAmendmentUnit = tag.getString("last_amendment_unit");
        cell.lastAmendmentConcentration = tag.contains("last_amendment_concentration", Tag.TAG_DOUBLE)
            ? Math.max(0, tag.getDouble("last_amendment_concentration")) : 1.0;
        // Only 1a cells need the one-time Living Soil baseline. 2a cells migrate directly into model/schema 3.
        cell.simulationBaselinePending = schema < 2 || model < 2;
        return cell;
    }

    private static int nonNegative(int value) { return Math.max(0, value); }
    private static double unit(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
