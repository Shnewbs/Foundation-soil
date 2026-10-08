package com.foundations.soil.amendment;

import java.util.Optional;

/** Built-in default definitions. Pack authors may override every value from config/foundations_soil/amendments. */
public final class Amendments {
    private Amendments() {}

    public static final AmendmentDefinition LIME = solid(
        "lime", AmendmentForm.SUSPENDABLE,
        0.000, 0.12, 0.000, 0.000, 0.004, 0.000, 0.002, 0.000, 0.000, 1.20,
        "Lime", "A pH-raising mineral amendment used to correct acidic soil.",
        0.70, 8.0, 2.0, 0.010);
    public static final AmendmentDefinition SULFUR = solid(
        "sulfur", AmendmentForm.SUSPENDABLE,
        0.000, -0.10, 0.000, 0.000, 0.000, -0.002, 0.001, 0.000, 0.000, 1.20,
        "Sulfur", "A pH-lowering mineral amendment used to correct alkaline soil.",
        0.55, 12.0, 2.0, 0.010);
    public static final AmendmentDefinition CLAY = solid(
        "clay", AmendmentForm.SUSPENDABLE,
        0.002, 0.00, -0.015, 0.005, 0.020, 0.004, 0.000, 0.000, 0.000, 0.80,
        "Clay", "A mineral texture amendment that trades some drainage for improved soil body and structure.",
        0.55, 18.0, 2.5, 0.008);
    public static final AmendmentDefinition COMPOST = solid(
        "compost", AmendmentForm.SUSPENDABLE,
        0.012, 0.00, 0.006, -0.004, 0.012, 0.012, 0.001, 0.020, 0.060, 1.00,
        "Compost", "A biological organic amendment that feeds soil biology and organic matter.",
        0.45, 14.0, 2.5, 0.006);
    public static final AmendmentDefinition BIOCHAR = solid(
        "biochar", AmendmentForm.SUSPENDABLE,
        0.010, 0.015, 0.010, -0.005, 0.020, 0.008, 0.000, 0.018, 0.004, 0.85,
        "Biochar", "A persistent carbon-rich amendment that supports structure, drainage and long-term soil condition.",
        0.30, 28.0, 3.0, 0.004);
    public static final AmendmentDefinition MANURE = solid(
        "manure", AmendmentForm.SUSPENDABLE,
        0.010, 0.005, -0.002, 0.002, 0.006, 0.012, 0.006, 0.022, 0.090, 1.10,
        "Manure", "A nutrient-rich biological amendment collected from adult TFC livestock.",
        0.50, 12.0, 2.0, 0.012);

    public static final AmendmentDefinition COMPOST_TEA = liquid(
        "compost_tea", AmendmentForm.LIQUID_SOLUTION,
        0.003, 0.000, 0.002, -0.001, 0.003, 0.018, 0.001, 0.004, 0.180, 0.35,
        "Compost Tea", "A dilute biological liquid amendment emphasizing short-term soil biology.",
        250.0, 1000.0, 0.75, 3.0, 4.0, 0.010);
    public static final AmendmentDefinition MANURE_SLURRY = liquid(
        "manure_slurry", AmendmentForm.LIQUID_SUSPENSION,
        0.005, 0.002, -0.001, 0.001, 0.003, 0.010, 0.008, 0.012, 0.100, 0.75,
        "Manure Slurry", "A suspended manure amendment with stronger organic reserve and salinity load.",
        250.0, 1000.0, 0.55, 7.0, 3.0, 0.018);
    public static final AmendmentDefinition LIQUID_LIME = liquid(
        "liquid_lime", AmendmentForm.LIQUID_SUSPENSION,
        0.000, 0.060, 0.000, 0.000, 0.002, 0.000, 0.002, 0.000, 0.000, 0.65,
        "Lime Suspension", "A liquid-delivered lime suspension for controlled pH correction.",
        250.0, 1000.0, 0.80, 4.0, 3.0, 0.012);
    public static final AmendmentDefinition SULFUR_SOLUTION = liquid(
        "sulfur_solution", AmendmentForm.LIQUID_SUSPENSION,
        0.000, -0.050, 0.000, 0.000, 0.000, -0.001, 0.002, 0.000, 0.000, 0.65,
        "Sulfur Suspension", "A water-carried fine sulfur suspension for gradual pH reduction.",
        250.0, 1000.0, 0.75, 5.0, 3.0, 0.012);
    public static final AmendmentDefinition BIOCHAR_SUSPENSION = liquid(
        "biochar_suspension", AmendmentForm.LIQUID_SUSPENSION,
        0.005, 0.008, 0.004, -0.002, 0.010, 0.006, 0.001, 0.010, 0.030, 0.50,
        "Biochar Suspension", "A suspended fine-biochar treatment with slow structural and biological effects.",
        250.0, 1000.0, 0.35, 12.0, 3.0, 0.008);

    private static AmendmentDefinition solid(String id, AmendmentForm form,
        double om, double ph, double drainage, double compaction, double structure, double biology, double salinity,
        double reserve, double decomposition, double diminishing, String name, String description,
        double immediateFraction, double releaseDays, double overThreshold, double overSalinity) {
        return new AmendmentDefinition(id, form, om, ph, drainage, compaction, structure, biology, salinity,
            reserve, decomposition, diminishing, name, description, AmendmentDoseUnit.APPLICATION,
            1.0, 4.0, 1.0, immediateFraction, releaseDays, overThreshold, overSalinity);
    }

    private static AmendmentDefinition liquid(String id, AmendmentForm form,
        double om, double ph, double drainage, double compaction, double structure, double biology, double salinity,
        double reserve, double decomposition, double diminishing, String name, String description,
        double referenceMb, double maxMb, double immediateFraction, double releaseDays,
        double overThreshold, double overSalinity) {
        return new AmendmentDefinition(id, form, om, ph, drainage, compaction, structure, biology, salinity,
            reserve, decomposition, diminishing, name, description, AmendmentDoseUnit.MILLIBUCKET,
            referenceMb, maxMb, 4.0, immediateFraction, releaseDays, overThreshold, overSalinity);
    }

    public static Optional<AmendmentDefinition> builtIn(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(switch (id) {
            case "lime" -> LIME;
            case "sulfur" -> SULFUR;
            case "clay" -> CLAY;
            case "compost" -> COMPOST;
            case "biochar" -> BIOCHAR;
            case "manure" -> MANURE;
            case "compost_tea" -> COMPOST_TEA;
            case "manure_slurry" -> MANURE_SLURRY;
            case "liquid_lime" -> LIQUID_LIME;
            case "sulfur_solution" -> SULFUR_SOLUTION;
            case "biochar_suspension" -> BIOCHAR_SUSPENSION;
            default -> null;
        });
    }

    public static AmendmentDefinition byId(String id) {
        return builtIn(id).orElse(COMPOST);
    }
}
