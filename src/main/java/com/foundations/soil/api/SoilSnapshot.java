package com.foundations.soil.api;

public record SoilSnapshot(
    int cellX,
    int cellZ,
    double organicMatter,
    double pH,
    double drainage,
    double compaction,
    double structure,
    double biology,
    double salinity,
    double nutrientAvailabilityModifier,
    long lastTfcTick,
    long lastSimulatedDay,
    int amendmentApplications,
    int cropCycles,
    int continuousCropCycles,
    double continuousCropPressure,
    long daysBare,
    long daysCovered,
    double organicReserve,
    int activePlantings,
    String lastHarvestFamily
) {}
