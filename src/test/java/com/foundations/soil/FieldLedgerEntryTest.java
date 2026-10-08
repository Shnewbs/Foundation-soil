package com.foundations.soil;

import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.ledger.FieldLedgerEntry;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class FieldLedgerEntryTest {
    @Test
    void snapshotAndPlayerMetadataRoundTrip() {
        SoilCell cell = new SoilCell(12, -4, 71,
            new SoilProfile(0.42, 6.35, 0.70, 0.11, 0.73, 0.61, 0.03), 500L, 22L);
        FieldLedgerEntry entry = new FieldLedgerEntry("minecraft:overworld", 12, -4, 71, 500L);
        entry.refresh(cell, 540L, 23L);
        entry.setFavorite(true);
        entry.setFieldId(3);
        entry.setNote("North wheat test plot");
        entry.addEvent(23L, "Soil Cell discovered with Soil Probe");
        entry.addEvent(23L, "Applied amendment: compost");

        FieldLedgerEntry loaded = FieldLedgerEntry.load(entry.save());
        assertEquals("minecraft:overworld", loaded.dimension());
        assertEquals(12, loaded.cellX());
        assertEquals(-4, loaded.cellZ());
        assertEquals(6.35, loaded.pH(), 0.0001);
        assertEquals(0.42, loaded.organicMatter(), 0.0001);
        assertTrue(loaded.favorite());
        assertEquals(3, loaded.fieldId());
        assertEquals("North wheat test plot", loaded.note());
        assertEquals(2, loaded.recentEvents().size());
        assertTrue(loaded.recentEvents().getFirst().contains("compost"));
    }


    @Test
    void legacyEntriesLoadWithoutInventingTfcData() {
        FieldLedgerEntry entry = new FieldLedgerEntry("minecraft:overworld", 0, 0, 64, 1L);
        var tag = entry.save();
        tag.remove("tfc_soil_block");
        tag.remove("tfc_farmland_data_present");
        tag.remove("tfc_hydration");
        tag.remove("tfc_min_hydration");
        tag.remove("tfc_max_hydration");
        tag.remove("tfc_nitrogen");
        tag.remove("tfc_phosphorus");
        tag.remove("tfc_potassium");

        FieldLedgerEntry loaded = FieldLedgerEntry.load(tag);
        assertEquals("", loaded.tfcSoilBlock());
        assertFalse(loaded.tfcFarmlandDataPresent());
        assertEquals(-1, loaded.tfcHydration());
        assertEquals(-1, loaded.tfcMinHydration());
        assertEquals(-1, loaded.tfcMaxHydration());
        assertEquals(0.0, loaded.tfcNitrogen(), 0.0001);
        assertEquals(0.0, loaded.tfcPhosphorus(), 0.0001);
        assertEquals(0.0, loaded.tfcPotassium(), 0.0001);
    }

    @Test
    void r4FarmlandSnapshotLoadsWithoutInventingHydrationRange() {
        FieldLedgerEntry entry = new FieldLedgerEntry("minecraft:overworld", 2, 3, 64, 1L);
        var tag = entry.save();
        tag.putString("tfc_soil_block", "tfc:farmland/entisol");
        tag.putBoolean("tfc_farmland_data_present", true);
        tag.putInt("tfc_hydration", 27);
        tag.putDouble("tfc_nitrogen", 0.12);
        tag.putDouble("tfc_phosphorus", 0.23);
        tag.putDouble("tfc_potassium", 0.34);
        tag.remove("tfc_min_hydration");
        tag.remove("tfc_max_hydration");

        FieldLedgerEntry loaded = FieldLedgerEntry.load(tag);
        assertTrue(loaded.tfcFarmlandDataPresent());
        assertEquals(27, loaded.tfcHydration());
        assertEquals(-1, loaded.tfcMinHydration());
        assertEquals(-1, loaded.tfcMaxHydration());
        assertEquals(0.12, loaded.tfcNitrogen(), 0.0001);
    }


    @Test
    void amendmentStatusRoundTripsWithDelayedReleaseMetadata() {
        SoilCell cell = new SoilCell(4, 5, 64,
            new SoilProfile(0.30, 6.50, 0.60, 0.12, 0.58, 0.42, 0.02), 10L, 10L);
        cell.applyDose(Amendments.COMPOST_TEA, 500.0, Amendments.COMPOST_TEA.doseUnit(), 1.0,
            11L, 10L, 0.22, 1.0, 32);
        FieldLedgerEntry entry = new FieldLedgerEntry("minecraft:overworld", 4, 5, 64, 10L);
        entry.refresh(cell, 11L, 10L);

        FieldLedgerEntry loaded = FieldLedgerEntry.load(entry.save());
        assertEquals("compost_tea", loaded.lastAmendmentId());
        assertEquals(10L, loaded.lastAmendmentDay());
        assertEquals(500.0, loaded.lastAmendmentAmount(), 0.0001);
        assertEquals("MILLIBUCKET", loaded.lastAmendmentUnit());
        assertEquals(1.0, loaded.lastAmendmentConcentration(), 0.0001);
        assertTrue(loaded.pendingAmendmentCount() > 0);
        assertTrue(loaded.pendingAmendmentDaysRemaining() > 0.0);
        assertTrue(loaded.recentAmendmentLoad() > 0.0);
    }

    @Test
    void recentHistoryIsBounded() {
        FieldLedgerEntry entry = new FieldLedgerEntry("minecraft:overworld", 0, 0, 64, 1L);
        for (int i = 0; i < 40; i++) entry.addEvent(i, "event-" + i);
        assertEquals(FieldLedgerEntry.MAX_RECENT_EVENTS, entry.recentEvents().size());
        assertTrue(entry.recentEvents().getFirst().contains("event-39"));
    }
}
