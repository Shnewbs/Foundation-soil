package com.foundations.soil.api;

/** Immutable game-facing tolerance inputs; does not store genomes or add a Genetics dependency. */
public record SoilTraits(double pHShift, double compactionToleranceBonus, double salinityTolerance,
                         double nutrientEfficiency, double waterloggingTolerance) {
    public static final SoilTraits NEUTRAL = new SoilTraits(0, 0, 0, 1, 0);
    public SoilTraits {
        pHShift = range(pHShift, -2, 2);
        compactionToleranceBonus = range(compactionToleranceBonus, -1, 1);
        salinityTolerance = range(salinityTolerance, 0, 1);
        nutrientEfficiency = range(nutrientEfficiency, .25, 2);
        waterloggingTolerance = range(waterloggingTolerance, 0, 1);
    }
    private static double range(double v, double lo, double hi) {
        if (!Double.isFinite(v) || v < lo || v > hi) throw new IllegalArgumentException("Invalid soil trait tolerance");
        return v;
    }
}
