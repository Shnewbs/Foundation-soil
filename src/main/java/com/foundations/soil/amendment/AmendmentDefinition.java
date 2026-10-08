package com.foundations.soil.amendment;

import java.util.Locale;

/**
 * Data-driven amendment definition.
 *
 * Effect values are expressed per reference dose. A delivery system only supplies the amendment id,
 * amount, unit and concentration; Foundations Soil owns normalization, diminishing response, delayed
 * release, overapplication handling and persistence.
 */
public record AmendmentDefinition(
    String id,
    AmendmentForm form,
    double organicMatter,
    double pH,
    double drainage,
    double compaction,
    double structure,
    double biology,
    double salinity,
    double organicReserve,
    double decompositionRate,
    double diminishingWeight,
    String displayName,
    String description,
    AmendmentDoseUnit doseUnit,
    double referenceAmount,
    double maxSingleAmount,
    double maxConcentration,
    double immediateFraction,
    double releaseDays,
    double overapplicationThreshold,
    double overapplicationSalinity
) {
    public AmendmentDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Amendment id cannot be blank");
        form = form == null ? AmendmentForm.DRY : form;
        organicMatter = finite("organicMatter", organicMatter);
        pH = finite("pH", pH);
        drainage = finite("drainage", drainage);
        compaction = finite("compaction", compaction);
        structure = finite("structure", structure);
        biology = finite("biology", biology);
        salinity = finite("salinity", salinity);
        organicReserve = Math.max(0.0, finite("organicReserve", organicReserve));
        decompositionRate = Math.max(0.0, finite("decompositionRate", decompositionRate));
        diminishingWeight = Math.max(0.0, finite("diminishingWeight", diminishingWeight));
        displayName = displayName == null || displayName.isBlank() ? title(id) : displayName.trim();
        description = description == null ? "" : description.trim();
        doseUnit = doseUnit == null ? defaultUnit(form) : doseUnit;
        referenceAmount = positive("referenceAmount", referenceAmount);
        maxSingleAmount = positive("maxSingleAmount", maxSingleAmount);
        if (maxSingleAmount < referenceAmount) throw new IllegalArgumentException("Amendment maxSingleAmount must be >= referenceAmount");
        maxConcentration = positive("maxConcentration", maxConcentration);
        immediateFraction = clamp(finite("immediateFraction", immediateFraction), 0.0, 1.0);
        releaseDays = Math.max(0.0, finite("releaseDays", releaseDays));
        if (releaseDays <= 0.0) immediateFraction = 1.0;
        overapplicationThreshold = Math.max(0.0, finite("overapplicationThreshold", overapplicationThreshold));
        overapplicationSalinity = Math.max(0.0, finite("overapplicationSalinity", overapplicationSalinity));
        if (form.isLiquid() && doseUnit != AmendmentDoseUnit.MILLIBUCKET) {
            throw new IllegalArgumentException("Liquid amendment " + id + " must use MILLIBUCKET dosing");
        }
    }

    /** Existing R6/R8 constructor; preserves a one-application, immediate solid definition. */
    public AmendmentDefinition(String id, AmendmentForm form, double organicMatter, double pH, double drainage,
                               double compaction, double structure, double biology, double salinity,
                               double organicReserve, double decompositionRate, double diminishingWeight,
                               String displayName, String description) {
        this(id, form, organicMatter, pH, drainage, compaction, structure, biology, salinity,
            organicReserve, decompositionRate, diminishingWeight, displayName, description,
            defaultUnit(form), defaultReference(form), defaultMax(form), form != null && form.isLiquid() ? 4.0 : 1.0,
            1.0, 0.0, 2.0, 0.010);
    }

    /** Backward-compatible constructor used by existing APIs/tests. */
    public AmendmentDefinition(String id, AmendmentForm form, double organicMatter, double pH, double drainage,
                               double compaction, double structure, double biology, double salinity,
                               double organicReserve, double decompositionRate, double diminishingWeight) {
        this(id, form, organicMatter, pH, drainage, compaction, structure, biology, salinity,
            organicReserve, decompositionRate, diminishingWeight, title(id), "");
    }

    public AmendmentDefinition scaled(double strength) {
        double s = Math.max(0.0, finite("strength", strength));
        return new AmendmentDefinition(id, form, organicMatter * s, pH * s, drainage * s, compaction * s,
            structure * s, biology * s, salinity * s, organicReserve * s, decompositionRate,
            diminishingWeight, displayName, description, doseUnit, referenceAmount, maxSingleAmount,
            maxConcentration, immediateFraction, releaseDays, overapplicationThreshold, overapplicationSalinity);
    }

    public double normalizedDose(double amount, AmendmentDoseUnit suppliedUnit, double concentration) {
        amount = positive("amount", amount);
        concentration = positive("concentration", concentration);
        if (suppliedUnit != doseUnit) {
            throw new IllegalArgumentException("Amendment " + id + " expects " + doseUnit + " but received " + suppliedUnit);
        }
        if (amount > maxSingleAmount + 1.0e-9) {
            throw new IllegalArgumentException("Amendment " + id + " amount " + amount + " exceeds max single amount " + maxSingleAmount);
        }
        if (!form.isLiquid() && Math.abs(concentration - 1.0) > 1.0e-9) {
            throw new IllegalArgumentException("Solid amendment " + id + " uses amount/application units; concentration must be 1.0");
        }
        if (concentration > maxConcentration + 1.0e-9) {
            throw new IllegalArgumentException("Amendment " + id + " concentration " + concentration + " exceeds max " + maxConcentration);
        }
        return (amount / referenceAmount) * concentration;
    }

    private static AmendmentDoseUnit defaultUnit(AmendmentForm form) {
        return form != null && form.isLiquid() ? AmendmentDoseUnit.MILLIBUCKET : AmendmentDoseUnit.APPLICATION;
    }

    private static double defaultReference(AmendmentForm form) { return form != null && form.isLiquid() ? 250.0 : 1.0; }
    private static double defaultMax(AmendmentForm form) { return form != null && form.isLiquid() ? 1000.0 : 4.0; }

    private static double finite(String name, double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Amendment " + name + " must be finite");
        return value;
    }

    private static double positive(String name, double value) {
        value = finite(name, value);
        if (value <= 0) throw new IllegalArgumentException("Amendment " + name + " must be > 0");
        return value;
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static String title(String id) {
        String value = id == null ? "Amendment" : id.replace('_', ' ').trim();
        if (value.isEmpty()) return "Amendment";
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
