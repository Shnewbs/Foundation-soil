package com.foundations.soil.amendment;

import java.util.Locale;

/** Unit used to normalize an amendment application against its data-defined reference dose. */
public enum AmendmentDoseUnit {
    APPLICATION,
    MILLIBUCKET;

    public static AmendmentDoseUnit parse(String value, AmendmentForm form) {
        if (value == null || value.isBlank()) return form != null && form.isLiquid() ? MILLIBUCKET : APPLICATION;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "APPLICATION", "APPLICATIONS", "USE", "USES", "DOSE", "DOSES" -> APPLICATION;
            case "MILLIBUCKET", "MILLIBUCKETS", "MB", "M_B" -> MILLIBUCKET;
            default -> throw new IllegalArgumentException("Unknown amendment dose unit: " + value);
        };
    }

    public String shortName() {
        return this == MILLIBUCKET ? "mB" : "application";
    }
}
