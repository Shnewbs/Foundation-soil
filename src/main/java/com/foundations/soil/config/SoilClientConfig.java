package com.foundations.soil.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only presentation controls. No authoritative soil state is stored here. */
public final class SoilClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue HUD_ENABLED;
    public static final ModConfigSpec.BooleanValue HUD_SHOW_WITH_HOE;
    public static final ModConfigSpec.BooleanValue HUD_EXACT_WITH_PROBE;
    public static final ModConfigSpec.IntValue HUD_X_OFFSET;
    public static final ModConfigSpec.IntValue HUD_Y_OFFSET;
    public static final ModConfigSpec.IntValue HUD_LINE_SPACING;

    public static final ModConfigSpec.BooleanValue FIELD_GUIDE_DARKEN_BACKGROUND;
    public static final ModConfigSpec.BooleanValue FIELD_GUIDE_SHOW_SEARCH;
    public static final ModConfigSpec.BooleanValue FIELD_GUIDE_SHOW_PAGE_NUMBERS;
    public static final ModConfigSpec.BooleanValue FIELD_GUIDE_SHOW_WARNINGS;
    public static final ModConfigSpec.BooleanValue FIELD_GUIDE_SHOW_COORDINATES;
    public static final ModConfigSpec.BooleanValue FIELD_GUIDE_SHOW_CHUNK_COORDINATES;
    public static final ModConfigSpec.IntValue FIELD_GUIDE_TITLE_X_OFFSET;
    public static final ModConfigSpec.IntValue FIELD_GUIDE_TITLE_Y_OFFSET;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("hud");
        HUD_ENABLED = b.define("enabled", true);
        HUD_SHOW_WITH_HOE = b.define("show_with_hoe", true);
        HUD_EXACT_WITH_PROBE = b.define("show_exact_with_probe", true);
        HUD_X_OFFSET = b.defineInRange("x_offset", 0, -1000, 1000);
        HUD_Y_OFFSET = b.defineInRange("y_offset", 0, -1000, 1000);
        HUD_LINE_SPACING = b.defineInRange("line_spacing", 1, 0, 8);
        b.pop();

        b.push("field_guide");
        FIELD_GUIDE_DARKEN_BACKGROUND = b.comment("Darken the world behind the guide. The book/art layer itself is never blurred.")
            .define("darken_background", true);
        FIELD_GUIDE_SHOW_SEARCH = b.define("show_search", true);
        FIELD_GUIDE_SHOW_PAGE_NUMBERS = b.define("show_page_numbers", true);
        FIELD_GUIDE_SHOW_WARNINGS = b.define("show_warning_badges", true);
        FIELD_GUIDE_SHOW_COORDINATES = b.define("show_coordinates", true);
        FIELD_GUIDE_SHOW_CHUNK_COORDINATES = b.define("show_chunk_coordinates", true);
        FIELD_GUIDE_TITLE_X_OFFSET = b.defineInRange("title_x_offset", 0, -200, 200);
        FIELD_GUIDE_TITLE_Y_OFFSET = b.defineInRange("title_y_offset", 0, -100, 100);
        b.pop();

        SPEC = b.build();
    }

    private SoilClientConfig() {}
}
