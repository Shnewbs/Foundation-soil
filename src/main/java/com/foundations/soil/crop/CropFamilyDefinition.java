package com.foundations.soil.crop;

/** Data-driven crop-family agronomy used for default crop preferences and harvest soil effects. */
public record CropFamilyDefinition(
    CropDefinition crop,
    double residueReserve,
    double organicMatterDelta,
    double compactionDelta,
    double structureDelta,
    double biologyDelta,
    double salinityDelta
) {
    public CropFamilyDefinition {
        if (crop == null) throw new IllegalArgumentException("crop definition cannot be null");
        residueReserve = Math.max(0.0, finite("residueReserve", residueReserve));
        organicMatterDelta = finite("organicMatterDelta", organicMatterDelta);
        compactionDelta = finite("compactionDelta", compactionDelta);
        structureDelta = finite("structureDelta", structureDelta);
        biologyDelta = finite("biologyDelta", biologyDelta);
        salinityDelta = finite("salinityDelta", salinityDelta);
    }

    private static double finite(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Crop-family " + name + " must be finite");
        return value;
    }
}
