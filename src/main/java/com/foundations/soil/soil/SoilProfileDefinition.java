package com.foundations.soil.soil;

/** Initial deterministic profile for a matching TFC/vanilla soil block path. */
public record SoilProfileDefinition(
    String id,
    String pathContains,
    SoilProfile base,
    double organicMatterJitter,
    double pHJitter,
    double drainageJitter,
    double compactionJitter,
    double structureJitter,
    double biologyJitter,
    double salinityJitter
) {
    public SoilProfileDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("soil profile id cannot be blank");
        pathContains = pathContains == null ? "" : pathContains.toLowerCase(java.util.Locale.ROOT);
        if (base == null) throw new IllegalArgumentException("base profile cannot be null");
        organicMatterJitter = nonNegative("organicMatterJitter", organicMatterJitter);
        pHJitter = nonNegative("pHJitter", pHJitter);
        drainageJitter = nonNegative("drainageJitter", drainageJitter);
        compactionJitter = nonNegative("compactionJitter", compactionJitter);
        structureJitter = nonNegative("structureJitter", structureJitter);
        biologyJitter = nonNegative("biologyJitter", biologyJitter);
        salinityJitter = nonNegative("salinityJitter", salinityJitter);
    }

    public boolean matches(String path) {
        return !pathContains.isBlank() && path != null && path.toLowerCase(java.util.Locale.ROOT).contains(pathContains);
    }

    private static double nonNegative(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Soil profile " + name + " must be finite");
        return Math.max(0.0, value);
    }
}
