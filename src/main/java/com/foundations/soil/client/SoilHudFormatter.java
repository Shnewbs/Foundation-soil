package com.foundations.soil.client;

import com.foundations.soil.network.SoilHudSnapshotPayload;
import com.foundations.soil.soil.SoilLabels;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SoilHudFormatter {
    private SoilHudFormatter() {}

    public static List<String> simple(SoilHudSnapshotPayload p) {
        return List.of(
            "Organic Matter: " + SoilLabels.amount(p.organicMatter())
                + "   pH: " + SoilLabels.pH(p.pH())
                + "   Drainage: " + SoilLabels.quality(p.drainage()),
            "Compaction: " + SoilLabels.compaction(p.compaction())
                + "   Structure: " + SoilLabels.quality(p.structure())
                + "   Biology: " + SoilLabels.amount(p.biology())
                + "   Salinity: " + SoilLabels.amount(p.salinity())
        );
    }

    /** Full Soil Probe diagnostics: TFC's authoritative farmland data first, then Foundations Soil. */
    public static List<String> detailed(SoilHudSnapshotPayload p) {
        ArrayList<String> lines = new ArrayList<>();
        String soil = tfcSoilLabel(p.tfcSoilBlock());
        if (p.tfcFarmlandDataPresent()) {
            String range = p.tfcMinHydration() >= 0 && p.tfcMaxHydration() >= 0
                ? String.format(Locale.ROOT, " (Min %d%% Max %d%%)", p.tfcMinHydration(), p.tfcMaxHydration())
                : "";
            lines.add(String.format(Locale.ROOT, "TFC Soil: %s   Hydration %d%%%s", soil, p.tfcHydration(), range));
            lines.add(String.format(Locale.ROOT,
                "(N) Nitrogen %.2f%%   (P) Phosphorus %.2f%%   (K) Potassium %.2f%%",
                pct(p.tfcNitrogen()), pct(p.tfcPhosphorus()), pct(p.tfcPotassium())));
        } else if (!p.tfcSoilBlock().isBlank()) {
            lines.add("TFC Soil: " + soil + "   Hydration/N/P/K require farmland");
        }

        String family = p.lastHarvestFamily().isBlank() ? "none" : p.lastHarvestFamily();
        lines.add(String.format(Locale.ROOT, "OM %.1f%%   pH %.2f   Drainage %.1f%%   Compaction %.1f%%",
            pct(p.organicMatter()), p.pH(), pct(p.drainage()), pct(p.compaction())));
        lines.add(String.format(Locale.ROOT, "Structure %.1f%%   Biology %.1f%%   Salinity %.1f%%",
            pct(p.structure()), pct(p.biology()), pct(p.salinity())));
        lines.add(String.format(Locale.ROOT, "Nutrient Access x%.2f   Pressure %.1f%%   Organic Reserve %.1f%%",
            p.nutrientAccess(), pct(p.continuousPressure()), pct(p.organicReserve())));
        lines.add(String.format(Locale.ROOT, "Soil Cell [%d, %d]   Crops %d   Active %d   Last %s   Amendments %d",
            p.cellX(), p.cellZ(), p.cropCycles(), p.activePlantings(), family, p.amendmentApplications()));
        return List.copyOf(lines);
    }

    private static String tfcSoilLabel(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            String kind = titleWords(parts[parts.length - 2]);
            String variant = titleWords(parts[parts.length - 1]);
            if (kind.equalsIgnoreCase("Farmland")) return variant + " Farmland";
            return variant + " " + kind;
        }
        return titleWords(path);
    }

    private static String titleWords(String value) {
        String[] words = value.replace('_', ' ').trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static double pct(double value) { return value * 100.0; }
}
