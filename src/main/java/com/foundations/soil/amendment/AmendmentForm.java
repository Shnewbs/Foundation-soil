package com.foundations.soil.amendment;

import java.util.Locale;

/** Physical amendment form. Delivery/transport remains owned by other mods or player items. */
public enum AmendmentForm {
    DRY(false),
    WATER_SOLUBLE(false),
    SUSPENDABLE(false),
    LIQUID_SOLUTION(true),
    LIQUID_SUSPENSION(true);

    private final boolean liquid;

    AmendmentForm(boolean liquid) { this.liquid = liquid; }

    public boolean isLiquid() { return liquid; }
    public boolean isSolid() { return !liquid; }

    public static AmendmentForm parse(String value) {
        if (value == null || value.isBlank()) return DRY;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "LIQUID", "SOLUTION", "LIQUID_SOLUTION" -> LIQUID_SOLUTION;
            case "SLURRY", "LIQUID_SUSPENSION" -> LIQUID_SUSPENSION;
            default -> {
                try { yield valueOf(normalized); }
                catch (IllegalArgumentException ignored) { yield DRY; }
            }
        };
    }
}
