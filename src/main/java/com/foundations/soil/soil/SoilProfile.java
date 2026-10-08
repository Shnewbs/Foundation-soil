package com.foundations.soil.soil;

import com.foundations.soil.amendment.AmendmentDefinition;

public record SoilProfile(
    double organicMatter,
    double pH,
    double drainage,
    double compaction,
    double structure,
    double biology,
    double salinity
) {
    public SoilProfile {
        organicMatter = unit(finite("organicMatter", organicMatter));
        pH = clamp(finite("pH", pH), 3.5, 9.5);
        drainage = unit(finite("drainage", drainage));
        compaction = unit(finite("compaction", compaction));
        structure = unit(finite("structure", structure));
        biology = unit(finite("biology", biology));
        salinity = unit(finite("salinity", salinity));
    }

    public SoilProfile apply(AmendmentDefinition a) { return apply(a, 1.0); }

    public SoilProfile apply(AmendmentDefinition a, double scale) {
        if (a == null) throw new IllegalArgumentException("amendment cannot be null");
        scale = finite("scale", scale);
        return adjust(
            a.organicMatter() * scale,
            a.pH() * scale,
            a.drainage() * scale,
            a.compaction() * scale,
            a.structure() * scale,
            a.biology() * scale,
            a.salinity() * scale
        );
    }

    public SoilProfile adjust(double om, double ph, double drainageDelta, double compactionDelta,
                              double structureDelta, double biologyDelta, double salinityDelta) {
        return new SoilProfile(
            organicMatter + finite("organicMatterDelta", om),
            pH + finite("pHDelta", ph),
            drainage + finite("drainageDelta", drainageDelta),
            compaction + finite("compactionDelta", compactionDelta),
            structure + finite("structureDelta", structureDelta),
            biology + finite("biologyDelta", biologyDelta),
            salinity + finite("salinityDelta", salinityDelta)
        );
    }

    public SoilProfile withDynamics(double om, double compaction, double structure, double biology, double salinity) {
        return new SoilProfile(om, pH, drainage, compaction, structure, biology, salinity);
    }

    public double nutrientAvailabilityModifier() {
        double ph = 1.0 - Math.min(0.45, Math.abs(pH - 6.5) * 0.09);
        double rootAccess = 1.0 - compaction * 0.35;
        double biological = 0.88 + biology * 0.20;
        double condition = 0.88 + structure * 0.12;
        return clamp(ph * rootAccess * biological * condition, 0.35, 1.08);
    }

    private static double finite(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Soil " + name + " must be finite");
        return value;
    }
    private static double unit(double value) { return clamp(value, 0.0, 1.0); }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
