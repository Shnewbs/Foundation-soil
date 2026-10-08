package com.foundations.soil.crop;

import com.foundations.soil.soil.SoilProfile;

public record CropDefinition(
    CropFamily family,
    double preferredPHMin,
    double preferredPHMax,
    double compactionTolerance,
    double drainageMinimum,
    double drainagePreferred,
    double organicMatterResponse
) {
    public CropDefinition {
        family = family == null ? CropFamily.OTHER : family;
        preferredPHMin = finite("preferredPHMin", preferredPHMin);
        preferredPHMax = finite("preferredPHMax", preferredPHMax);
        compactionTolerance = finite("compactionTolerance", compactionTolerance);
        drainageMinimum = finite("drainageMinimum", drainageMinimum);
        drainagePreferred = finite("drainagePreferred", drainagePreferred);
        organicMatterResponse = finite("organicMatterResponse", organicMatterResponse);
        if (preferredPHMin > preferredPHMax) {
            double t = preferredPHMin;
            preferredPHMin = preferredPHMax;
            preferredPHMax = t;
        }
        preferredPHMin = clamp(preferredPHMin, 3.5, 9.5);
        preferredPHMax = clamp(preferredPHMax, 3.5, 9.5);
        compactionTolerance = unit(compactionTolerance);
        drainageMinimum = unit(drainageMinimum);
        drainagePreferred = unit(drainagePreferred);
        organicMatterResponse = unit(organicMatterResponse);
    }

    public double soilResponse(SoilProfile p) {
        double ph = rangeScore(p.pH(), preferredPHMin, preferredPHMax, 2.0);
        double compact = p.compaction() <= compactionTolerance ? 1.0
            : clamp(1.0 - (p.compaction() - compactionTolerance) * 1.35, 0.25, 1.0);
        double drain = p.drainage() < drainageMinimum
            ? clamp(p.drainage() / Math.max(0.05, drainageMinimum), 0.25, 1.0)
            : 1.0 - Math.min(0.25, Math.abs(p.drainage() - drainagePreferred) * 0.25);
        double om = 0.85 + p.organicMatter() * 0.15 * Math.max(0.2, organicMatterResponse);
        return clamp(ph * compact * drain * om, 0.20, 1.05);
    }

    private static double rangeScore(double value, double min, double max, double falloff) {
        if (value >= min && value <= max) return 1.0;
        double distance = value < min ? min - value : value - max;
        return clamp(1.0 - distance / falloff, 0.30, 1.0);
    }

    private static double finite(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Crop " + name + " must be finite");
        return value;
    }
    private static double unit(double v) { return clamp(v, 0, 1); }
    private static double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}
