package com.foundations.soil.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative gameplay and persistence tuning for Foundations Soil. */
public final class SoilConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue TFC_YIELD_RESPONSE_STRENGTH;
    public static final ModConfigSpec.ConfigValue<String> PRESET;
    public static final ModConfigSpec.BooleanValue DAILY_UPDATES;
    public static final ModConfigSpec.IntValue SIMULATION_BATCH_SIZE;
    public static final ModConfigSpec.DoubleValue SIMULATION_RATE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ORGANIC_MATTER_DECAY_RATE;
    public static final ModConfigSpec.DoubleValue BIOLOGY_RESPONSE_RATE;
    public static final ModConfigSpec.DoubleValue COMPACTION_RECOVERY_RATE;
    public static final ModConfigSpec.DoubleValue STRUCTURE_RESPONSE_RATE;
    public static final ModConfigSpec.DoubleValue SALINITY_LEACH_RATE;
    public static final ModConfigSpec.DoubleValue CROP_PRESSURE_DECAY_RATE;
    public static final ModConfigSpec.DoubleValue AMENDMENT_LOAD_DECAY_RATE;

    public static final ModConfigSpec.IntValue APPLICATIONS_PER_BAG;
    public static final ModConfigSpec.DoubleValue AMENDMENT_STRENGTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AMENDMENT_DIMINISHING_STRENGTH;
    public static final ModConfigSpec.BooleanValue AMENDMENT_ENGINE_ENABLED;
    public static final ModConfigSpec.IntValue MAX_PENDING_AMENDMENT_RELEASES;
    public static final ModConfigSpec.DoubleValue MAX_NORMALIZED_AMENDMENT_DOSE;
    public static final ModConfigSpec.IntValue MANURE_COLLECTION_COOLDOWN_DAYS;

    public static final ModConfigSpec.DoubleValue TRAMPLE_COMPACTION;
    public static final ModConfigSpec.DoubleValue TRAMPLE_STRUCTURE;
    public static final ModConfigSpec.DoubleValue TILL_COMPACTION;
    public static final ModConfigSpec.DoubleValue TILL_STRUCTURE;
    public static final ModConfigSpec.DoubleValue PLANTING_ROTATION_RELIEF;
    public static final ModConfigSpec.DoubleValue HARVEST_ROTATION_RELIEF;
    public static final ModConfigSpec.DoubleValue CROP_PRESSURE_BASE;
    public static final ModConfigSpec.DoubleValue CROP_PRESSURE_REPEAT;
    public static final ModConfigSpec.DoubleValue CROP_PRESSURE_REPEAT_CAP;

    public static final ModConfigSpec.IntValue LEDGER_MAX_RECENT_EVENTS;
    public static final ModConfigSpec.IntValue LEDGER_MAX_SYNC_CELLS;
    public static final ModConfigSpec.IntValue LEDGER_MAX_SYNC_FIELDS;
    public static final ModConfigSpec.IntValue LEDGER_EVENTS_PER_CELL_SYNC;

    public static final ModConfigSpec.DoubleValue WARNING_LOW_ORGANIC_MATTER;
    public static final ModConfigSpec.DoubleValue WARNING_HIGH_COMPACTION;
    public static final ModConfigSpec.DoubleValue WARNING_HIGH_SALINITY;
    public static final ModConfigSpec.DoubleValue WARNING_HIGH_CROP_PRESSURE;
    public static final ModConfigSpec.DoubleValue WARNING_ACIDIC_PH;
    public static final ModConfigSpec.DoubleValue WARNING_ALKALINE_PH;
    public static final ModConfigSpec.DoubleValue WARNING_LOW_BIOLOGY;

    public static final ModConfigSpec.BooleanValue WRITE_DEFAULT_DEFINITIONS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("crop_response");
        TFC_YIELD_RESPONSE_STRENGTH = b.comment("Blend applied only to newly accumulated TFC crop yield. 0 disables the bridge; 1 applies the full Soil response.",
            "Does not replace TFC growth, expiry, hydration, or stored N/P/K. Native pack/API compatibility testing is still required for beta.")
            .defineInRange("tfc_yield_response_strength", 0.5, 0.0, 1.0);
        b.pop();
        b.push("balance");
        PRESET = b.comment(
                "High-level balance preset. Supported values: realistic, balanced, accelerated_testing.",
                "The preset multiplies long-duration simulation rates; individual values below remain editable.")
            .define("preset", "realistic");
        SIMULATION_RATE_MULTIPLIER = b.comment("Additional multiplier applied after the preset multiplier.")
            .defineInRange("simulation_rate_multiplier", 1.0, 0.0, 100.0);
        b.pop();

        b.push("simulation");
        DAILY_UPDATES = b.comment("Update persistent Soil Cells on TFC calendar day changes.")
            .define("daily_updates", true);
        SIMULATION_BATCH_SIZE = b.comment("Maximum Soil Cells advanced per server tick while a TFC-day update is pending.")
            .defineInRange("batch_size", 250, 16, 10000);
        b.pop();

        b.push("rates");
        ORGANIC_MATTER_DECAY_RATE = rate(b, "organic_matter_decay", 0.00008);
        BIOLOGY_RESPONSE_RATE = rate(b, "biology_response", 0.030);
        COMPACTION_RECOVERY_RATE = rate(b, "compaction_recovery", 0.0012);
        STRUCTURE_RESPONSE_RATE = rate(b, "structure_response", 0.0060);
        SALINITY_LEACH_RATE = rate(b, "salinity_leach", 0.0040);
        CROP_PRESSURE_DECAY_RATE = rate(b, "crop_pressure_decay", 0.0040);
        AMENDMENT_LOAD_DECAY_RATE = rate(b, "amendment_load_decay", 0.060);
        b.pop();

        b.push("amendments");
        APPLICATIONS_PER_BAG = b.comment("Number of soil applications provided by a filled amendment bag. Requires a restart after editing.")
            .defineInRange("applications_per_bag", 8, 1, 64);
        AMENDMENT_STRENGTH_MULTIPLIER = b.comment("Global multiplier for amendment stat/reserve effects loaded from JSON definitions.")
            .defineInRange("strength_multiplier", 1.0, 0.0, 10.0);
        AMENDMENT_DIMINISHING_STRENGTH = b.comment("How strongly recent repeated amendment applications reduce the next application.")
            .defineInRange("diminishing_strength", 0.22, 0.0, 2.0);
        AMENDMENT_ENGINE_ENABLED = b.comment("Master switch for server-authoritative solid/liquid amendment applications.")
            .define("engine_enabled", true);
        MAX_PENDING_AMENDMENT_RELEASES = b.comment("Maximum persisted delayed-release amendment residues per Soil Cell. Same-id residues are merged before this limit is used.")
            .defineInRange("max_pending_releases", 32, 1, 128);
        MAX_NORMALIZED_AMENDMENT_DOSE = b.comment("Global safety ceiling for one normalized amendment application after amount/concentration conversion.")
            .defineInRange("max_normalized_dose", 8.0, 0.25, 64.0);
        MANURE_COLLECTION_COOLDOWN_DAYS = b.comment("Minimum TFC calendar days before the same adult livestock can provide another manure unit.")
            .defineInRange("manure_collection_cooldown_days", 1, 1, 30);
        b.pop();

        b.push("field_history");
        TRAMPLE_COMPACTION = delta(b, "trample_compaction", 0.030);
        TRAMPLE_STRUCTURE = delta(b, "trample_structure", -0.018);
        TILL_COMPACTION = delta(b, "till_compaction", 0.003);
        TILL_STRUCTURE = delta(b, "till_structure", -0.002);
        PLANTING_ROTATION_RELIEF = b.comment("Multiplier applied to existing pressure when planting a different crop family.")
            .defineInRange("planting_rotation_relief", 0.82, 0.0, 1.0);
        HARVEST_ROTATION_RELIEF = b.comment("Multiplier applied to existing pressure after a harvested crop family changes.")
            .defineInRange("harvest_rotation_relief", 0.62, 0.0, 1.0);
        CROP_PRESSURE_BASE = b.comment("Pressure added for every mature crop cycle.")
            .defineInRange("crop_pressure_base", 0.060, 0.0, 1.0);
        CROP_PRESSURE_REPEAT = b.comment("Additional pressure per repeated continuous crop-family cycle.")
            .defineInRange("crop_pressure_repeat", 0.040, 0.0, 1.0);
        CROP_PRESSURE_REPEAT_CAP = b.comment("Maximum extra pressure added by repeated cycles.")
            .defineInRange("crop_pressure_repeat_cap", 0.100, 0.0, 1.0);
        b.pop();

        b.push("field_ledger");
        LEDGER_MAX_RECENT_EVENTS = b.comment("Maximum recent history lines persisted per discovered Soil Cell.")
            .defineInRange("max_recent_events", 12, 1, 64);
        LEDGER_MAX_SYNC_CELLS = b.comment("Maximum Soil Cells included in one Field Guide network snapshot.")
            .defineInRange("max_sync_cells", 512, 32, 2048);
        LEDGER_MAX_SYNC_FIELDS = b.comment("Maximum named fields included in one Field Guide network snapshot.")
            .defineInRange("max_sync_fields", 128, 8, 512);
        LEDGER_EVENTS_PER_CELL_SYNC = b.comment("Maximum recent history lines per cell sent to the Field Guide client.")
            .defineInRange("events_per_cell_sync", 6, 0, 16);
        b.pop();

        b.push("warnings");
        WARNING_LOW_ORGANIC_MATTER = unit(b, "low_organic_matter", 0.20);
        WARNING_HIGH_COMPACTION = unit(b, "high_compaction", 0.55);
        WARNING_HIGH_SALINITY = unit(b, "high_salinity", 0.40);
        WARNING_HIGH_CROP_PRESSURE = unit(b, "high_crop_pressure", 0.50);
        WARNING_ACIDIC_PH = b.defineInRange("acidic_ph", 5.5, 3.5, 9.5);
        WARNING_ALKALINE_PH = b.defineInRange("alkaline_ph", 8.0, 3.5, 9.5);
        WARNING_LOW_BIOLOGY = unit(b, "low_biology", 0.18);
        b.pop();

        b.push("definitions");
        WRITE_DEFAULT_DEFINITIONS = b.comment(
                "Create editable JSON definition files under config/foundations_soil when they are missing.",
                "Existing files are never overwritten automatically.")
            .define("write_defaults", true);
        b.pop();

        SPEC = b.build();
    }

    private SoilConfig() {}

    private static ModConfigSpec.DoubleValue rate(ModConfigSpec.Builder b, String name, double value) {
        return b.defineInRange(name, value, 0.0, 1.0);
    }

    private static ModConfigSpec.DoubleValue delta(ModConfigSpec.Builder b, String name, double value) {
        return b.defineInRange(name, value, -1.0, 1.0);
    }

    private static ModConfigSpec.DoubleValue unit(ModConfigSpec.Builder b, String name, double value) {
        return b.defineInRange(name, value, 0.0, 1.0);
    }

    public static String preset() {
        String value = PRESET.get();
        if (value == null) return "realistic";
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "balanced" -> "balanced";
            case "accelerated", "accelerated_testing", "testing" -> "accelerated_testing";
            default -> "realistic";
        };
    }

    public static double presetRateMultiplier() {
        return switch (preset()) {
            case "balanced" -> 1.50;
            case "accelerated_testing" -> 12.0;
            default -> 1.0;
        };
    }

    public static double effectiveSimulationMultiplier() {
        return presetRateMultiplier() * SIMULATION_RATE_MULTIPLIER.get();
    }

    public static double effectiveAmendmentStrength() {
        double preset = switch (preset()) {
            case "accelerated_testing" -> 1.5;
            default -> 1.0;
        };
        return preset * AMENDMENT_STRENGTH_MULTIPLIER.get();
    }
}
