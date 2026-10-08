package com.foundations.soil.api;

import com.foundations.soil.soil.SoilCell;

/** Immutable snapshot conversion shared by the public API, events and integration layers. */
public final class SoilSnapshots {
    private SoilSnapshots() {}

    public static SoilSnapshot from(SoilCell cell) {
        if (cell == null) throw new IllegalArgumentException("cell cannot be null");
        var p = cell.profile();
        return new SoilSnapshot(cell.cellX(), cell.cellZ(), p.organicMatter(), p.pH(), p.drainage(),
            p.compaction(), p.structure(), p.biology(), p.salinity(), cell.nutrientAccessModifier(),
            cell.lastTfcTick(), cell.lastSimulatedDay(), cell.amendmentApplications(), cell.cropCycles(),
            cell.continuousCropCycles(), cell.continuousPressure(), cell.daysBare(), cell.daysCovered(),
            cell.organicReserve() + cell.residueReserve(), cell.activePlantings(), cell.lastHarvestFamily());
    }
}
