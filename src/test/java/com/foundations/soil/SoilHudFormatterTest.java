package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.client.SoilHudFormatter;
import com.foundations.soil.network.SoilHudSnapshotPayload;
import org.junit.jupiter.api.Test;

class SoilHudFormatterTest {
    private static final SoilHudSnapshotPayload SAMPLE = new SoilHudSnapshotPayload(
        1L, 12, 4,
        "tfc:farmland/entisol", true, 27, 15, 38, 0.12, 0.23, 0.34,
        .42, 6.20, .71, .12, .68, .55, .02, .91, 3,
        .14, .06, 8, 5, "grain"
    );

    @Test void hoeHudStaysQualitative() {
        String text = String.join(" | ", SoilHudFormatter.simple(SAMPLE));
        assertTrue(text.contains("Organic Matter: Moderate"));
        assertTrue(text.contains("pH: Slightly Acidic"));
        assertFalse(text.contains("Soil Cell"));
        assertFalse(text.contains("Nutrient Access"));
        assertFalse(text.contains("Pressure"));
        assertFalse(text.contains("TFC Soil"));
    }

    @Test void probeHudShowsTfcAndPrecisionLivingSoilDiagnostics() {
        String text = String.join(" | ", SoilHudFormatter.detailed(SAMPLE));
        assertTrue(text.contains("TFC Soil: Entisol Farmland"));
        assertTrue(text.contains("Hydration 27% (Min 15% Max 38%)"));
        assertTrue(text.contains("Nitrogen 12.00%"));
        assertTrue(text.contains("Phosphorus 23.00%"));
        assertTrue(text.contains("Potassium 34.00%"));
        assertTrue(text.contains("OM 42.0%"));
        assertTrue(text.contains("pH 6.20"));
        assertTrue(text.contains("Nutrient Access x0.91"));
        assertTrue(text.contains("Pressure 14.0%"));
        assertTrue(text.contains("Organic Reserve 6.0%"));
        assertTrue(text.contains("Soil Cell [12, 4]"));
        assertTrue(text.contains("Last grain"));
    }
}
