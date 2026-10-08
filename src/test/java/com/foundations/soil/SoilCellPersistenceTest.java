package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilProfile;
import com.foundations.soil.soil.SoilSimulationContext;
import com.foundations.soil.soil.SoilSimulator;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SoilCellPersistenceTest {
    @Test void roundTripsLivingSoilProfileAndHistory() {
        SoilCell cell = new SoilCell(12, -4, 71, new SoilProfile(.42, 6.25, .71, .12, .68, .55, .02), 1234L, 10L);
        cell.apply(Amendments.COMPOST, 1400L, 10L, .22);
        cell.recordPlanting(CropFamily.GRAIN, 1500L, 10L, 71);
        cell.recordCropRemoval(CropFamily.GRAIN, true, 1600L, 11L);

        SoilCell loaded = SoilCell.load(cell.save());
        assertEquals(12, loaded.cellX());
        assertEquals(-4, loaded.cellZ());
        assertEquals(71, loaded.sampleY());
        assertEquals(1600L, loaded.lastTfcTick());
        assertEquals(1, loaded.amendmentApplications());
        assertEquals(1, loaded.cropCycles());
        assertEquals("grain", loaded.lastHarvestFamily());
        assertTrue(loaded.organicReserve() > 0);
        assertEquals(cell.pendingAmendmentCount(), loaded.pendingAmendmentCount());
        assertEquals(cell.profile().organicMatter(), loaded.profile().organicMatter(), 0.000001);
    }

    @Test void schemaOneCellMigratesWithSafeHistoryDefaults() {
        CompoundTag tag = new SoilCell(0, 0, new SoilProfile(.3, 6.5, .6, .1, .6, .5, .01), 1L).save();
        tag.putInt("schema", 1);
        tag.putInt("model", 1);
        tag.remove("sample_y");
        tag.remove("last_simulated_day");
        tag.remove("crop_cycles");
        SoilCell loaded = SoilCell.load(tag);
        assertEquals(64, loaded.sampleY());
        assertEquals(0L, loaded.lastSimulatedDay());
        assertEquals(0, loaded.cropCycles());
    }

    @Test void legacyOneACellBaselinesBeforeLivingSimulation() {
        SoilProfile original = new SoilProfile(.3, 6.5, .6, .1, .6, .5, .01);
        CompoundTag tag = new SoilCell(0, 0, original, 24000L * 50L).save();
        tag.putInt("schema", 1);
        tag.putInt("model", 1);
        tag.remove("last_simulated_day");
        SoilCell loaded = SoilCell.load(tag);
        assertTrue(loaded.simulationBaselinePending());

        SoilSimulator.Rates rates = new SoilSimulator.Rates(.5, .5, .5, .5, .5, .5, .5);
        assertTrue(SoilSimulator.advance(loaded, 500L, 24000L * 500L, new SoilSimulationContext(35, 1.0), rates));
        assertFalse(loaded.simulationBaselinePending());
        assertEquals(500L, loaded.lastSimulatedDay());
        assertEquals(original.organicMatter(), loaded.profile().organicMatter(), 0.000001);
        assertEquals(original.biology(), loaded.profile().biology(), 0.000001);
    }

    @Test void refusesFutureSchemaInsteadOfSilentlyRewritingIt() {
        CompoundTag tag = new SoilCell(0, 0, new SoilProfile(.3, 6.5, .6, .1, .6, .5, .01), 1L).save();
        tag.putInt("schema", SoilCell.SCHEMA_VERSION + 1);
        assertThrows(IllegalStateException.class, () -> SoilCell.load(tag));
    }
}
