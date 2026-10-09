package com.foundations.soil.ledger;

import com.foundations.soil.soil.SoilPhysics;
import com.foundations.soil.soil.SoilTrend;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/** Snapshot-only analysis: no chunk loading, world mutation, or another player's ledger. */
public final class SoilLaboratoryReports {
    private SoilLaboratoryReports() {}
    public static List<String> describe(PlayerFieldLedger ledger, LaboratorySelection selected) {
        if (selected.field()) {
            var field = ledger.field(selected.fieldId()).orElse(null);
            if (field == null) return List.of("Choose one of your fields.");
            List<FieldLedgerEntry> members = ledger.sortedEntries().stream().filter(e -> e.fieldId() == field.id()).toList();
            ArrayList<String> out = new ArrayList<>();
            out.add(field.name()); out.add("Field #" + field.id() + ": " + members.size() + " discovered cells");
            if (members.isEmpty()) { out.add("Assign discovered cells in the Field Guide."); return List.copyOf(out); }
            long oldest = members.stream().mapToLong(FieldLedgerEntry::lastScannedDay).min().orElse(0);
            long newest = members.stream().mapToLong(FieldLedgerEntry::lastScannedDay).max().orElse(0);
            out.add("Soil snapshots: TFC days " + oldest + " to " + newest);
            out.add("Averages below use every field member, not just the current guide page.");
            out.add("Organic matter: " + percent(average(members, FieldLedgerEntry::organicMatter)));
            out.add("pH: " + number(average(members, FieldLedgerEntry::pH)));
            out.add("Compaction: " + percent(average(members, FieldLedgerEntry::compaction)));
            out.add("Structure: " + percent(average(members, FieldLedgerEntry::structure)));
            out.add("Biology: " + percent(average(members, FieldLedgerEntry::biology)));
            out.add("Salinity: " + percent(average(members, FieldLedgerEntry::salinity)));
            out.add("Continuous crop pressure: " + percent(average(members, FieldLedgerEntry::continuousPressure)));
            out.add("Nutrient access: x" + number(average(members, FieldLedgerEntry::nutrientAccess)));
            List<FieldLedgerEntry> nutrients = members.stream().filter(FieldLedgerEntry::tfcFarmlandDataPresent).toList();
            out.add("TFC nutrient snapshots available: " + nutrients.size() + "/" + members.size());
            if (!nutrients.isEmpty()) {
                out.add("Stored N / P / K: " + percent(average(nutrients, FieldLedgerEntry::tfcNitrogen)) + " / "
                    + percent(average(nutrients, FieldLedgerEntry::tfcPhosphorus)) + " / " + percent(average(nutrients, FieldLedgerEntry::tfcPotassium)));
                out.add("Effective N / P / K: " + percent(average(nutrients, e -> effective(e.tfcNitrogen(), e.nutrientAccess()))) + " / "
                    + percent(average(nutrients, e -> effective(e.tfcPhosphorus(), e.nutrientAccess()))) + " / "
                    + percent(average(nutrients, e -> effective(e.tfcPotassium(), e.nutrientAccess()))));
                out.add("Nutrients retain their last-probe timestamps; they are not live field measurements.");
            }
            long pending = members.stream().filter(e -> e.pendingAmendmentCount() > 0).count();
            out.add("Cells with delayed amendment effects: " + pending);
            out.add("Cells with overapplication recorded: " + members.stream().filter(FieldLedgerEntry::lastAmendmentOverApplied).count());
            List<FieldLedgerEntry> physical = members.stream().filter(e -> e.texture().isPresent()).toList();
            out.add("Physical composition available: " + physical.size() + "/" + members.size());
            if (!physical.isEmpty()) out.add("Sand / silt / clay: " + percent(average(physical, e -> e.texture().orElseThrow().sand())) + " / "
                + percent(average(physical, e -> e.texture().orElseThrow().silt())) + " / " + percent(average(physical, e -> e.texture().orElseThrow().clay())));
            TreeMap<Long, double[]> days = new TreeMap<>();
            for (FieldLedgerEntry entry : members) for (SoilTrend.Point point : entry.trendPoints()) {
                double[] values = days.computeIfAbsent(point.day(), ignored -> new double[4]);
                values[0]++; values[1] += point.profile().organicMatter(); values[2] += point.profile().pH(); values[3] += point.profile().biology();
                if (days.size() > SoilTrend.CAPACITY) days.pollFirstEntry();
            }
            out.add("ALIGNED FIELD TRENDS (complete membership only)");
            int complete = 0;
            for (var day : days.entrySet()) {
                double[] values = day.getValue();
                if (values[0] != members.size()) continue;
                complete++;
                out.add("Day " + day.getKey() + ": OM " + percent(values[1] / values[0]) + ", pH " + number(values[2] / values[0]) + ", biology " + percent(values[3] / values[0]));
            }
            if (complete == 0) out.add("No aligned whole-field observations yet. Inspect individual cells; no missing days are invented.");
            if (!field.note().isBlank()) { out.add("Note:"); out.add(field.note()); }
            return List.copyOf(out);
        }
        FieldLedgerEntry e = ledger.entry(selected.cellKey()).orElse(null);
        if (e == null) return List.of("Choose one of your discovered soil cells.", "Probe soil first, then compare its saved observations here.");
        ArrayList<String> out = new ArrayList<>();
        out.add("Cell " + e.cellX() + ", " + e.cellZ()); out.add(e.dimension());
        out.add("Soil state: TFC day " + e.lastScannedDay());
        out.add("TFC probe: " + (e.lastProbeDay() < 0 ? "legacy / unknown timestamp" : "day " + e.lastProbeDay()));
        out.add("Soil and TFC values are timestamped snapshots, not remote live measurements.");
        out.add("Organic matter: " + percent(e.organicMatter())); out.add("pH: " + number(e.pH()));
        out.add("Drainage quality: " + percent(e.drainage())); out.add("Compaction: " + percent(e.compaction()));
        out.add("Structure: " + percent(e.structure())); out.add("Biology: " + percent(e.biology())); out.add("Salinity: " + percent(e.salinity()));
        out.add("Nutrient access: x" + number(e.nutrientAccess()));
        if (e.tfcFarmlandDataPresent()) {
            out.add("TFC STORED / EFFECTIVE NUTRIENTS");
            out.add("Nitrogen: " + percent(e.tfcNitrogen()) + " / " + percent(effective(e.tfcNitrogen(), e.nutrientAccess())));
            out.add("Phosphorus: " + percent(e.tfcPhosphorus()) + " / " + percent(effective(e.tfcPhosphorus(), e.nutrientAccess())));
            out.add("Potassium: " + percent(e.tfcPotassium()) + " / " + percent(effective(e.tfcPotassium(), e.nutrientAccess())));
        } else out.add("No stored TFC farmland nutrient measurement in this snapshot.");
        if (e.texture().isPresent()) {
            var texture = e.texture().orElseThrow();
            out.add("PHYSICAL MODEL"); out.add("Texture: " + texture.textureClass().replace('_', ' '));
            out.add("Sand / silt / clay: " + percent(texture.sand()) + " / " + percent(texture.silt()) + " / " + percent(texture.clay()));
            if (e.tfcHydration() >= 0) {
                var physics = SoilPhysics.evaluate(texture, e.profileSnapshot(), e.tfcHydration() / 100.0);
                out.add("TFC hydration (last probe): " + e.tfcHydration() + "%");
                out.add("Water holding: " + percent(physics.waterHolding())); out.add("Field capacity index: " + percent(physics.fieldCapacity()));
                out.add("Aeration: " + percent(physics.aeration())); out.add("Root resistance: " + percent(physics.rootResistance()));
                out.add("Effective drainage: " + percent(physics.effectiveDrainage()));
                out.add("Root-zone estimate: " + physics.moisture().name().toLowerCase(Locale.ROOT));
                out.add("This interprets the last measured hydration; it does not create a second water store.");
            }
        } else out.add("Re-probe this legacy cell to record its physical composition.");
        out.add("MANAGEMENT");
        out.add("Crop cycles: " + e.cropCycles() + "; active plantings: " + e.activePlantings());
        out.add("Last harvested family: " + (e.lastHarvestFamily().isBlank() ? "none" : e.lastHarvestFamily()));
        out.add("Continuous pressure: " + percent(e.continuousPressure()));
        out.add("Days bare / covered: " + e.daysBare() + " / " + e.daysCovered());
        out.add("Organic and residue reserve: " + percent(e.organicReserve()));
        out.add("Amendment applications: " + e.amendmentApplications());
        out.add("Last amendment: " + (e.lastAmendmentId().isBlank() ? "none" : e.lastAmendmentId()));
        out.add("Delayed effects: " + e.pendingAmendmentCount() + "; up to " + number(e.pendingAmendmentDaysRemaining()) + " TFC days at snapshot");
        if (e.lastAmendmentOverApplied()) out.add("WARNING: last amendment was overapplied.");
        if (e.salinity() >= .15) out.add("Management: reduce saline inputs; improve drainage before adding more amendments.");
        if (e.compaction() >= .45) out.add("Management: avoid wet-soil traffic and rebuild structure gradually.");
        if (e.continuousPressure() >= .35) out.add("Management: rotate crop families or allow fallow recovery.");
        out.add("OBSERVED TFC-DAY TRENDS (maximum 16)");
        var points = e.trendPoints();
        if (points.size() > 1) {
            var a = points.getFirst().profile(); var b = points.getLast().profile();
            out.add("Change since day " + points.getFirst().day() + ": OM " + signed(b.organicMatter() - a.organicMatter()) + " pp; pH " + String.format(Locale.ROOT, "%+.2f", b.pH() - a.pH()));
        }
        for (var point : points) out.add("Day " + point.day() + ": OM " + percent(point.profile().organicMatter())
            + ", pH " + number(point.profile().pH()) + ", biology " + percent(point.profile().biology()));
        if (points.isEmpty()) out.add("No observations yet. Probe this cell to begin monitoring.");
        out.add("Gaps are unobserved intervals, not interpolated measurements.");
        if (!e.note().isBlank()) { out.add("Note:"); out.add(e.note()); }
        return List.copyOf(out);
    }
    private static double average(List<FieldLedgerEntry> entries, ToDoubleFunction<FieldLedgerEntry> value) { return entries.stream().mapToDouble(value).average().orElse(0); }
    private static double effective(double stored, double access) { return Math.max(0, Math.min(1, stored * access)); }
    private static String percent(double value) { return String.format(Locale.ROOT, "%.1f%%", value * 100); }
    private static String number(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static String signed(double value) { return String.format(Locale.ROOT, "%+.1f", value * 100); }
}
