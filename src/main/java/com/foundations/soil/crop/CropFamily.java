package com.foundations.soil.crop;

import java.util.Locale;

public enum CropFamily {
    GRAIN,
    LEGUME,
    ROOT,
    VEGETABLE,
    FIBER,
    OILSEED,
    BOTANICAL,
    COVER_CROP,
    OTHER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CropFamily parse(String value) {
        if (value == null || value.isBlank()) return OTHER;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return OTHER;
        }
    }
}
