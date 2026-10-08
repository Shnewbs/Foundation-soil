package com.foundations.soil.amendment;

/** Direct SoilProfile deltas produced by an amendment. */
public record AmendmentEffect(
    double organicMatter,
    double pH,
    double drainage,
    double compaction,
    double structure,
    double biology,
    double salinity
) {
    public AmendmentEffect {
        organicMatter = finite("organicMatter", organicMatter);
        pH = finite("pH", pH);
        drainage = finite("drainage", drainage);
        compaction = finite("compaction", compaction);
        structure = finite("structure", structure);
        biology = finite("biology", biology);
        salinity = finite("salinity", salinity);
    }

    public static AmendmentEffect from(AmendmentDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("definition cannot be null");
        return new AmendmentEffect(definition.organicMatter(), definition.pH(), definition.drainage(),
            definition.compaction(), definition.structure(), definition.biology(), definition.salinity());
    }

    public AmendmentEffect scale(double scale) {
        if (!Double.isFinite(scale)) throw new IllegalArgumentException("scale must be finite");
        return new AmendmentEffect(organicMatter * scale, pH * scale, drainage * scale, compaction * scale,
            structure * scale, biology * scale, salinity * scale);
    }

    public AmendmentEffect plus(AmendmentEffect other) {
        if (other == null) return this;
        return new AmendmentEffect(organicMatter + other.organicMatter, pH + other.pH, drainage + other.drainage,
            compaction + other.compaction, structure + other.structure, biology + other.biology, salinity + other.salinity);
    }

    public boolean effectivelyZero() {
        return Math.abs(organicMatter) < 1.0e-12 && Math.abs(pH) < 1.0e-12 && Math.abs(drainage) < 1.0e-12
            && Math.abs(compaction) < 1.0e-12 && Math.abs(structure) < 1.0e-12
            && Math.abs(biology) < 1.0e-12 && Math.abs(salinity) < 1.0e-12;
    }

    private static double finite(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Amendment effect " + name + " must be finite");
        return value;
    }
}
