package com.foundations.soil.soil;

/** Geological fractions, not fertilizer amounts. Values are normalized once at the boundary. */
public record SoilTexture(double sand, double silt, double clay) {
    public static final SoilTexture LOAM = new SoilTexture(.40, .40, .20);

    public SoilTexture {
        if (!Double.isFinite(sand) || !Double.isFinite(silt) || !Double.isFinite(clay)
            || sand < 0 || silt < 0 || clay < 0) {
            throw new IllegalArgumentException("Texture fractions must be finite and non-negative");
        }
        // Scale first to avoid overflow for finite, but very large, extension inputs.
        double maximum = Math.max(sand, Math.max(silt, clay));
        if (maximum == 0) throw new IllegalArgumentException("Texture must contain a positive fraction");
        double sum = sand + silt + clay;
        // Preserve already normalized values bit-for-bit across repeated world save/load cycles.
        if (!Double.isFinite(sum) || Math.abs(sum - 1.0) > 1.0e-12) {
            sand /= maximum; silt /= maximum; clay /= maximum;
            double total = sand + silt + clay;
            sand /= total; silt /= total; clay /= total;
        }
    }

    public String textureClass() {
        if (clay >= .45) return "clay";
        if (clay >= .30) return "clay_loam";
        if (sand >= .75) return "sand";
        if (sand >= .52) return "sandy_loam";
        if (silt >= .50) return "silt_loam";
        return "loam";
    }

    /** A game-scale classification of TFC's named soils, not a laboratory particle-size standard. */
    public static SoilTexture forSoil(String soilId) {
        String id = soilId == null ? "" : soilId.toLowerCase(java.util.Locale.ROOT);
        if (id.contains("clay_loam")) return new SoilTexture(.32, .30, .38);
        if (id.contains("clay")) return new SoilTexture(.20, .25, .55);
        if (id.contains("sandy_loam")) return new SoilTexture(.65, .25, .10);
        if (id.contains("sand")) return new SoilTexture(.82, .12, .06);
        if (id.contains("silt")) return new SoilTexture(.20, .65, .15);
        return LOAM;
    }
}
