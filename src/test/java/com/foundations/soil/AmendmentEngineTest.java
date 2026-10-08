package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;

import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilProfile;
import com.foundations.soil.soil.SoilSimulationContext;
import com.foundations.soil.soil.SoilSimulator;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class AmendmentEngineTest {
    private static final SoilProfile BASE = new SoilProfile(.30, 6.20, .60, .15, .55, .40, .02);

    @Test
    void liquidDoseNormalizesByMilliBucketsAndQueuesResidual() {
        SoilCell cell = new SoilCell(0, 0, 64, BASE, 1L, 10L);
        var result = cell.applyDose(Amendments.COMPOST_TEA, 250.0, AmendmentDoseUnit.MILLIBUCKET, 1.0,
            2L, 10L, .22, 1.0, 32);
        assertEquals(1.0, result.normalizedDose(), 0.000001);
        assertEquals(.75, result.immediateFraction(), 0.000001);
        assertEquals(1, cell.pendingAmendmentCount());
        assertEquals("compost_tea", cell.lastAmendmentId());
    }

    @Test
    void delayedDirectEffectsReleaseAcrossTfcDaysAndPersist() {
        SoilCell cell = new SoilCell(0, 0, 64, BASE, 1L, 10L);
        var result = cell.applyDose(Amendments.LIQUID_LIME, 250.0, AmendmentDoseUnit.MILLIBUCKET, 1.0,
            2L, 10L, .22, 1.0, 32);
        assertEquals(1, result.pendingReleases());
        double afterImmediate = cell.profile().pH();
        assertTrue(afterImmediate > BASE.pH());

        SoilCell reloaded = SoilCell.load(cell.save());
        assertEquals(1, reloaded.pendingAmendmentCount());
        SoilSimulator.Rates zero = new SoilSimulator.Rates(0, 0, 0, 0, 0, 0, 0);
        assertTrue(SoilSimulator.advance(reloaded, 14L, 100L, new SoilSimulationContext(22, .62), zero));
        assertEquals(0, reloaded.pendingAmendmentCount());
        assertTrue(reloaded.profile().pH() > afterImmediate);
        assertEquals(BASE.pH() + Amendments.LIQUID_LIME.pH(), reloaded.profile().pH(), 0.000001);
    }

    @Test
    void wrongDoseUnitAndUnsafeConcentrationAreRejected() {
        SoilCell cell = new SoilCell(0, 0, 64, BASE, 1L, 10L);
        assertThrows(IllegalArgumentException.class, () ->
            cell.applyDose(Amendments.COMPOST_TEA, 1.0, AmendmentDoseUnit.APPLICATION, 1.0, 2L, 10L, .22, 1.0, 32));
        assertThrows(IllegalArgumentException.class, () ->
            cell.applyDose(Amendments.COMPOST_TEA, 250.0, AmendmentDoseUnit.MILLIBUCKET, 5.0, 2L, 10L, .22, 1.0, 32));
    }

    @Test
    void solidDoseRejectsArtificialConcentrationMultiplier() {
        SoilCell cell = new SoilCell(0, 0, 64, BASE, 1L, 10L);
        assertThrows(IllegalArgumentException.class, () ->
            cell.applyDose(Amendments.COMPOST, 1.0, AmendmentDoseUnit.APPLICATION, 2.0, 2L, 10L, .22, 1.0, 32));
    }

    @Test
    void overapplicationAddsConfiguredSalinityPenalty() {
        SoilCell cell = new SoilCell(0, 0, 64, BASE, 1L, 10L);
        var result = cell.applyDose(Amendments.COMPOST_TEA, 1000.0, AmendmentDoseUnit.MILLIBUCKET, 2.0,
            2L, 10L, 0.0, 1.0, 32);
        assertTrue(result.overApplied());
        assertEquals(8.0, result.normalizedDose(), 0.000001);
        assertTrue(result.overapplicationSalinity() > 0);
        assertTrue(cell.profile().salinity() > BASE.salinity());
    }

    @Test
    void schemaTwoMigratesWithoutRetroactiveBaselineAndWithNoResidues() {
        SoilCell source = new SoilCell(2, 3, 64, BASE, 5L, 20L);
        CompoundTag tag = source.save();
        tag.putInt("schema", 2);
        tag.putInt("model", 2);
        tag.remove("pending_amendments");
        tag.remove("last_amendment_id");
        SoilCell loaded = SoilCell.load(tag);
        assertFalse(loaded.simulationBaselinePending());
        assertEquals(0, loaded.pendingAmendmentCount());
        assertEquals(20L, loaded.lastSimulatedDay());
    }
}
