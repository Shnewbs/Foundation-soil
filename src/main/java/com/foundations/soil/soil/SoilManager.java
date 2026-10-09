package com.foundations.soil.soil;

import com.foundations.soil.amendment.AmendmentApplicationOutcome;
import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.api.SoilSnapshots;
import com.foundations.soil.api.event.SoilEvents;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.ledger.FieldLedgerManager;
import java.util.Optional;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class SoilManager {
    private SoilManager() {}

    private static SoilWorldData data(ServerLevel level) {
        return com.foundations.soil.storage.SafeSavedData.get(level, SoilWorldData.DATA_NAME, SoilWorldData::new, SoilWorldData::load);
    }

    public static Optional<SoilCell> peek(ServerLevel level, BlockPos pos) {
        return data(level).get(SoilCellKey.from(pos));
    }

    public static SoilCell getOrCreate(ServerLevel level, BlockPos soilPos) {
        SoilCellKey key = SoilCellKey.from(soilPos);
        SoilWorldData world = data(level);
        long tick = Calendars.SERVER.getTicks();
        long day = Calendars.SERVER.getTotalCalendarDays();
        SoilCell cell = world.get(key).orElse(null);
        boolean created = cell == null;
        if (created) {
            cell = world.put(key, new SoilCell(key.cellX(), key.cellZ(), soilPos.getY(),
                SoilInitializer.create(level, soilPos), tick, day));
        }
        if (!cell.physicalInitialized()) {
            String soilId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(soilPos).getBlock()).toString();
            cell.initializePhysical(SoilTexture.forSoil(soilId), soilId);
            world.setDirty();
        }
        if (created) SoilEvents.bus().post(new SoilEvents.CellInitialized(level.dimension().location(), soilPos.immutable(), SoilSnapshots.from(cell)));
        if (cell.sampleY() != soilPos.getY()) world.setDirty();
        cell.touch(tick, soilPos.getY());
        long beforeDay = cell.lastSimulatedDay();
        if (advance(level, cell, day, tick, soilPos)) {
            world.setDirty();
            fireSimulation(level, soilPos, cell, beforeDay, day);
        }
        return cell;
    }

    public static SoilCell applyAmendment(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment) {
        return applyAmendmentDose(level, soilPos, amendment, amendment.referenceAmount(), amendment.doseUnit(), 1.0, "legacy").cell();
    }

    public static AmendmentApplyResult applyAmendmentDose(ServerLevel level, BlockPos soilPos, AmendmentDefinition amendment,
                                                           double amount, AmendmentDoseUnit unit, double concentration,
                                                           String source) {
        if (!SoilConfig.AMENDMENT_ENGINE_ENABLED.get()) throw new IllegalStateException("Foundations Soil amendment engine is disabled by server config");
        if (amendment == null) throw new IllegalArgumentException("amendment cannot be null");
        if (unit == null) throw new IllegalArgumentException("unit cannot be null");
        double normalized = amendment.normalizedDose(amount, unit, concentration);
        if (normalized > SoilConfig.MAX_NORMALIZED_AMENDMENT_DOSE.get() + 1.0e-9) {
            throw new IllegalArgumentException("Normalized amendment dose " + normalized + " exceeds server maximum " + SoilConfig.MAX_NORMALIZED_AMENDMENT_DOSE.get());
        }
        SoilCell cell = getOrCreate(level, soilPos);
        long tick = Calendars.SERVER.getTicks();
        long day = Calendars.SERVER.getTotalCalendarDays();
        AmendmentApplicationOutcome outcome = cell.applyDose(amendment, amount, unit, concentration, tick, day,
            SoilConfig.AMENDMENT_DIMINISHING_STRENGTH.get(), SoilConfig.effectiveAmendmentStrength(),
            SoilConfig.MAX_PENDING_AMENDMENT_RELEASES.get());
        data(level).setDirty();
        var snapshot = SoilSnapshots.from(cell);
        String actualSource = source == null || source.isBlank() ? "unknown" : source;
        SoilEvents.bus().post(new SoilEvents.AmendmentApplied(level.dimension().location(), soilPos.immutable(),
            amendment.id(), outcome.effectiveness(), snapshot));
        SoilEvents.bus().post(new SoilEvents.AmendmentDoseApplied(level.dimension().location(), soilPos.immutable(),
            amendment.id(), amendment.form().name().toLowerCase(java.util.Locale.ROOT), amount, unit.name().toLowerCase(java.util.Locale.ROOT),
            concentration, outcome.normalizedDose(), outcome.effectiveness(), outcome.overApplied(), actualSource, snapshot));
        SoilEvents.bus().post(new SoilEvents.CellChanged(level.dimension().location(), soilPos.immutable(),
            "amendment:" + amendment.id(), snapshot));
        String event = String.format(java.util.Locale.ROOT, "Applied %s: %.2f %s @ %.2fx%s", amendment.displayName(),
            amount, unit.shortName(), concentration, outcome.overApplied() ? " [OVERAPPLIED]" : "");
        FieldLedgerManager.refreshAllDiscovered(level.getServer(), level, cell, event, outcome.overApplied());
        return new AmendmentApplyResult(cell, outcome);
    }

    public record AmendmentApplyResult(SoilCell cell, AmendmentApplicationOutcome outcome) {}

    public static SoilCell recordPlanting(ServerLevel level, BlockPos soilPos, CropFamily family) {
        SoilCell cell = getOrCreate(level, soilPos);
        CropFamily actual = family == null ? CropFamily.OTHER : family;
        if (!cell.markPlanting(soilPos.getX(), soilPos.getZ(), true)) return cell;
        cell.recordPlanting(actual, Calendars.SERVER.getTicks(), Calendars.SERVER.getTotalCalendarDays(), soilPos.getY());
        data(level).setDirty();
        var snapshot = SoilSnapshots.from(cell);
        SoilEvents.bus().post(new SoilEvents.CropPlanted(level.dimension().location(), soilPos.immutable(), actual.id(), snapshot));
        SoilEvents.bus().post(new SoilEvents.CellChanged(level.dimension().location(), soilPos.immutable(), "crop_planted", snapshot));
        return cell;
    }

    public static SoilCell recordCropRemoval(ServerLevel level, BlockPos soilPos, CropFamily family, boolean mature) {
        SoilCell cell = getOrCreate(level, soilPos);
        CropFamily actual = family == null ? CropFamily.OTHER : family;
        if (!cell.markPlanting(soilPos.getX(), soilPos.getZ(), false)) return cell;
        cell.recordCropRemoval(actual, mature, Calendars.SERVER.getTicks(), Calendars.SERVER.getTotalCalendarDays());
        data(level).setDirty();
        var snapshot = SoilSnapshots.from(cell);
        SoilEvents.bus().post(new SoilEvents.CropRemoved(level.dimension().location(), soilPos.immutable(), actual.id(), mature, snapshot));
        SoilEvents.bus().post(new SoilEvents.CellChanged(level.dimension().location(), soilPos.immutable(),
            mature ? "crop_harvested" : "crop_removed", snapshot));
        return cell;
    }

    public static void recordPickingHarvest(ServerLevel level, BlockPos soilPos, CropFamily family) {
        SoilCell cell = getOrCreate(level, soilPos);
        CropFamily actual = family == null ? CropFamily.OTHER : family;
        if (cell.markPlanting(soilPos.getX(), soilPos.getZ(), true)) {
            cell.reconcilePlantings(cell.activePlantings() + 1, actual.id());
        }
        cell.recordPickingHarvest(actual, Calendars.SERVER.getTicks(), Calendars.SERVER.getTotalCalendarDays());
        data(level).setDirty();
        fireChanged(level, soilPos, cell, "crop_picked");
    }

    public static SoilCell recordDisturbance(ServerLevel level, BlockPos soilPos, double compaction, double structure) {
        SoilCell cell = getOrCreate(level, soilPos);
        cell.recordDisturbance(compaction, structure, Calendars.SERVER.getTicks(), Calendars.SERVER.getTotalCalendarDays(), soilPos.getY());
        data(level).setDirty();
        fireChanged(level, soilPos, cell, "disturbance");
        return cell;
    }

    public static SoilCell reset(ServerLevel level, BlockPos soilPos) {
        SoilCell cell = getOrCreate(level, soilPos);
        cell.resetTo(SoilInitializer.create(level, soilPos), soilPos.getY(), Calendars.SERVER.getTicks(), Calendars.SERVER.getTotalCalendarDays());
        data(level).setDirty();
        fireChanged(level, soilPos, cell, "reset");
        return cell;
    }

    public static SoilCell setStat(ServerLevel level, BlockPos soilPos, String stat, double value) {
        SoilCell cell = getOrCreate(level, soilPos);
        SoilProfile p = cell.profile();
        SoilProfile next = switch (stat) {
            case "organic_matter" -> new SoilProfile(value, p.pH(), p.drainage(), p.compaction(), p.structure(), p.biology(), p.salinity());
            case "ph" -> new SoilProfile(p.organicMatter(), value, p.drainage(), p.compaction(), p.structure(), p.biology(), p.salinity());
            case "drainage" -> new SoilProfile(p.organicMatter(), p.pH(), value, p.compaction(), p.structure(), p.biology(), p.salinity());
            case "compaction" -> new SoilProfile(p.organicMatter(), p.pH(), p.drainage(), value, p.structure(), p.biology(), p.salinity());
            case "structure" -> new SoilProfile(p.organicMatter(), p.pH(), p.drainage(), p.compaction(), value, p.biology(), p.salinity());
            case "biology" -> new SoilProfile(p.organicMatter(), p.pH(), p.drainage(), p.compaction(), p.structure(), value, p.salinity());
            case "salinity" -> new SoilProfile(p.organicMatter(), p.pH(), p.drainage(), p.compaction(), p.structure(), p.biology(), value);
            default -> throw new IllegalArgumentException("Unknown soil stat: " + stat);
        };
        cell.replaceProfile(next, Calendars.SERVER.getTicks(), Calendars.SERVER.getTotalCalendarDays(), soilPos.getY());
        data(level).setDirty();
        fireChanged(level, soilPos, cell, "set:" + stat);
        return cell;
    }

    public static void enableMonitoring(ServerLevel level, SoilCell cell) {
        cell.enableMonitoring(); data(level).setDirty();
    }

    public static int loadedCellCount(ServerLevel level) { return data(level).size(); }
    public static long[] packedCellKeys(ServerLevel level) { return data(level).packedKeysSnapshot(); }

    /** Read-only QA helper: returns a cloned cell advanced by the requested number of TFC days. */
    public static SoilCell previewDays(ServerLevel level, BlockPos soilPos, int days) {
        SoilCell source = getOrCreate(level, soilPos);
        SoilCell copy = SoilCell.load(source.save());
        long targetDay = copy.lastSimulatedDay() + Math.max(0, days);
        BlockPos sample = samplePos(copy);
        SoilSimulationContext env = environment(level, sample);
        SoilSimulator.advance(copy, targetDay, Calendars.SERVER.getTicks(), env, rates());
        return copy;
    }

    public static boolean simulatePacked(ServerLevel level, long packedKey, long currentDay) {
        SoilCell cell = data(level).getPacked(packedKey).orElse(null);
        if (cell == null) return false;
        BlockPos sample = samplePos(cell);
        if (!level.hasChunkAt(sample)) return false;
        long beforeDay = cell.lastSimulatedDay();
        boolean changed = advance(level, cell, currentDay, Calendars.SERVER.getTicks(), sample);
        if (changed) {
            data(level).setDirty();
            fireSimulation(level, sample, cell, beforeDay, currentDay);
        }
        return changed;
    }

    private static boolean advance(ServerLevel level, SoilCell cell, long currentDay, long tick, BlockPos samplePos) {
        if (!cell.simulationBaselinePending() && currentDay <= cell.lastSimulatedDay()) return false;
        if (!level.hasChunkAt(samplePos)) return false;
        reconcilePlantings(level, cell);
        return SoilSimulator.advance(cell, currentDay, tick, environment(level, samplePos), rates());
    }

    private static void fireSimulation(ServerLevel level, BlockPos pos, SoilCell cell, long fromDay, long toDay) {
        var snapshot = SoilSnapshots.from(cell);
        SoilEvents.bus().post(new SoilEvents.DailySimulation(level.dimension().location(), pos.immutable(), fromDay, toDay, snapshot));
        SoilEvents.bus().post(new SoilEvents.CellChanged(level.dimension().location(), pos.immutable(), "daily_simulation", snapshot));
    }

    private static void fireChanged(ServerLevel level, BlockPos pos, SoilCell cell, String reason) {
        SoilEvents.bus().post(new SoilEvents.CellChanged(level.dimension().location(), pos.immutable(), reason, SoilSnapshots.from(cell)));
    }

    private static void reconcilePlantings(ServerLevel level, SoilCell cell) {
        int baseX = cell.cellX() * SoilCellKey.CELL_SIZE;
        int baseZ = cell.cellZ() * SoilCellKey.CELL_SIZE;
        int y = cell.sampleY() + 1;
        int active = 0, presentMask = 0;
        String family = "";
        boolean mixed = false;
        for (int dx = 0; dx < SoilCellKey.CELL_SIZE; dx++) {
            for (int dz = 0; dz < SoilCellKey.CELL_SIZE; dz++) {
                var state = level.getBlockState(new BlockPos(baseX + dx, y, baseZ + dz));
                if (!CropRegistry.isCultivatedCrop(state)) continue;
                active++;
                presentMask |= 1 << (dz * 4 + dx);
                String next = CropRegistry.resolve(state).family().id();
                if (family.isBlank()) family = next;
                else if (!family.equals(next)) mixed = true;
            }
        }
        cell.reconcilePlantingMask(presentMask);
        cell.reconcilePlantings(active, mixed ? "mixed" : family);
    }

    private static SoilSimulationContext environment(ServerLevel level, BlockPos samplePos) {
        return new SoilSimulationContext(
            Climate.getInstantTemperature(level, samplePos),
            FarmlandBlock.getInstantHydration(level, samplePos) / 100.0
        );
    }

    private static SoilSimulator.Rates rates() {
        return new SoilSimulator.Rates(
            SoilConfig.ORGANIC_MATTER_DECAY_RATE.get() * SoilConfig.effectiveSimulationMultiplier(),
            SoilConfig.BIOLOGY_RESPONSE_RATE.get() * SoilConfig.effectiveSimulationMultiplier(),
            SoilConfig.COMPACTION_RECOVERY_RATE.get() * SoilConfig.effectiveSimulationMultiplier(),
            SoilConfig.STRUCTURE_RESPONSE_RATE.get() * SoilConfig.effectiveSimulationMultiplier(),
            SoilConfig.SALINITY_LEACH_RATE.get() * SoilConfig.effectiveSimulationMultiplier(),
            SoilConfig.CROP_PRESSURE_DECAY_RATE.get() * SoilConfig.effectiveSimulationMultiplier(),
            SoilConfig.AMENDMENT_LOAD_DECAY_RATE.get() * SoilConfig.effectiveSimulationMultiplier()
        );
    }

    public static BlockPos samplePos(SoilCell cell) {
        int x = cell.cellX() * SoilCellKey.CELL_SIZE + SoilCellKey.CELL_SIZE / 2;
        int z = cell.cellZ() * SoilCellKey.CELL_SIZE + SoilCellKey.CELL_SIZE / 2;
        return new BlockPos(x, cell.sampleY(), z);
    }
}
