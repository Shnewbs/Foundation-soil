package com.foundations.soil.soil;

public final class SoilSimulator {
    private SoilSimulator() {}

    public record Rates(
        double organicMatterDecay,
        double biologyResponse,
        double compactionRecovery,
        double structureResponse,
        double salinityLeach,
        double cropPressureDecay,
        double amendmentLoadDecay
    ) {}

    public static boolean advance(SoilCell cell, long currentDay, long tfcTick, SoilSimulationContext env, Rates rates) {
        if (cell.simulationBaselinePending()) {
            cell.establishSimulationBaseline(currentDay, tfcTick);
            return true;
        }
        long elapsed = currentDay - cell.lastSimulatedDay();
        if (elapsed <= 0) return false;

        double days = elapsed;
        // Release persisted amendment residues before the day's biological/structural response is evaluated.
        cell.releasePendingAmendments(days);
        SoilProfile p = cell.profile();
        double moisture = env.hydration();
        double tempFactor = clamp(1.0 - Math.abs(env.temperatureC() - 22.0) / 38.0, 0.08, 1.0);
        double moistureFactor = clamp(1.0 - Math.abs(moisture - 0.62) / 0.62, 0.10, 1.0);
        double phFactor = clamp(1.0 - Math.abs(p.pH() - 6.5) / 4.0, 0.20, 1.0);
        double salinityFactor = clamp(1.0 - p.salinity() * 0.80, 0.15, 1.0);

        double reserveRate = cell.organicReserveRate() * (0.25 + p.biology() * 0.75) * tempFactor * moistureFactor;
        double reserveReleased = cell.organicReserve() * (1.0 - Math.exp(-reserveRate * days));
        double residueReleased = cell.residueReserve() * (1.0 - Math.exp(-0.018 * (0.3 + p.biology()) * days));
        double newReserve = Math.max(0, cell.organicReserve() - reserveReleased);
        double newResidue = Math.max(0, cell.residueReserve() - residueReleased);

        double om = p.organicMatter() + reserveReleased * 0.70 + residueReleased * 0.55;
        om -= rates.organicMatterDecay() * (0.30 + p.biology() * 0.70) * tempFactor * Math.max(0.25, moistureFactor) * days;
        om = unit(om);

        double rotationFactor = clamp(1.0 - cell.continuousPressure() * 0.16, 0.78, 1.0);
        double biologyTarget = unit(
            0.08 + om * 0.42 + phFactor * 0.16 + moistureFactor * 0.18 + tempFactor * 0.16
        ) * salinityFactor * rotationFactor;
        double biology = approach(p.biology(), biologyTarget, rates.biologyResponse(), days);
        biology = unit(biology + (reserveReleased + residueReleased) * 0.08);

        double compaction = p.compaction();
        double recovery = rates.compactionRecovery() * (0.35 + biology * 0.65) * (0.55 + om * 0.45);
        if (cell.activePlantings() == 0) recovery *= 1.35;
        compaction = unit(compaction - recovery * days);

        int waterloggingEvents = cell.waterloggingEvents();
        if (moisture >= 0.95 && p.drainage() < 0.45) {
            compaction = unit(compaction + 0.0007 * days);
            waterloggingEvents += (int) Math.min(Integer.MAX_VALUE - (long) waterloggingEvents, elapsed);
        }

        double structureTarget = unit(0.30 + om * 0.36 + biology * 0.28 - compaction * 0.20);
        double structure = approach(p.structure(), structureTarget, rates.structureResponse(), days);

        double leach = rates.salinityLeach() * p.drainage() * Math.max(0.15, moisture) * days;
        double salinity = unit(p.salinity() * Math.exp(-Math.max(0, leach)));

        double pressureDecay = rates.cropPressureDecay() * (cell.activePlantings() == 0 ? 1.8 : 0.45) * days;
        double pressure = unit(cell.continuousPressure() * Math.exp(-Math.max(0, pressureDecay)));
        double amendmentLoad = Math.max(0, cell.recentAmendmentLoad() * Math.exp(-Math.max(0, rates.amendmentLoadDecay() * days)));

        SoilProfile next = p.withDynamics(om, compaction, structure, biology, salinity);
        long bare = cell.daysBare() + (cell.activePlantings() == 0 ? elapsed : 0);
        long covered = cell.daysCovered() + (cell.activePlantings() > 0 ? elapsed : 0);
        cell.setSimulationState(next, currentDay, tfcTick, bare, covered, waterloggingEvents, pressure,
            newReserve, newResidue, amendmentLoad);
        return true;
    }

    private static double approach(double current, double target, double rate, double days) {
        if (rate <= 0) return current;
        double weight = 1.0 - Math.exp(-rate * days);
        return current + (target - current) * weight;
    }

    private static double unit(double v) { return clamp(v, 0, 1); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
