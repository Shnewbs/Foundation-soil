package com.foundations.soil.soil;

/** Derived root-zone interpretation. This never stores or changes TFC hydration or nutrients. */
public record SoilPhysics(double waterHolding, double fieldCapacity, double aeration,
                          double rootResistance, double effectiveDrainage, double availableWater,
                          Moisture moisture) {
    public enum Moisture { DRY, LIMITED, OPTIMAL, WET, WATERLOGGED }

    public static SoilPhysics evaluate(SoilTexture texture, SoilProfile profile, double tfcHydration) {
        if (texture == null || profile == null || !Double.isFinite(tfcHydration))
            throw new IllegalArgumentException("Soil physics requires valid texture, profile and hydration");
        double hydration = unit(tfcHydration);
        double holding = unit(.15 + .50 * texture.silt() + .40 * texture.clay() + .20 * profile.organicMatter());
        double capacity = unit(.12 + .60 * holding + .12 * profile.structure());
        double aeration = unit(.25 + .60 * texture.sand() + .20 * profile.structure()
            - .35 * profile.compaction() - .20 * texture.clay());
        double resistance = unit(.60 * profile.compaction() + .25 * texture.clay() + .15 * (1 - profile.structure()));
        double drainage = unit(.55 * profile.drainage() + .35 * texture.sand() + .10 * profile.structure()
            - .20 * texture.clay() - .25 * profile.compaction());
        double available = unit(hydration * (.60 + .60 * holding) - .12 * profile.salinity());
        Moisture condition = hydration >= .90 && drainage < .35 ? Moisture.WATERLOGGED
            : available < .18 ? Moisture.DRY : available < .35 ? Moisture.LIMITED
            : hydration >= .85 ? Moisture.WET : Moisture.OPTIMAL;
        return new SoilPhysics(holding, capacity, aeration, resistance, drainage, available, condition);
    }

    public double responseModifier() {
        double water = switch (moisture) {
            case DRY -> .65;
            case LIMITED -> .85;
            case WATERLOGGED -> .70;
            default -> 1.0;
        };
        return water * (1.0 - .15 * rootResistance);
    }
    private static double unit(double v) { return Math.max(0, Math.min(1, v)); }
}
