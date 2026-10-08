package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilProfile;
import com.foundations.soil.soil.SoilSimulationContext;
import com.foundations.soil.soil.SoilSimulator;
import com.foundations.soil.crop.CropFamily;
import org.junit.jupiter.api.Test;

class SoilSimulationTest {
    private static final SoilSimulator.Rates RATES = new SoilSimulator.Rates(.00008, .03, .0012, .006, .004, .004, .06);

    @Test void elapsedTfcDaysAdvanceSoilWithoutTickByTickLoops() {
        SoilCell cell = new SoilCell(0, 0, 64, new SoilProfile(.30, 6.4, .60, .20, .50, .35, .08), 1L, 100L);
        cell.apply(Amendments.COMPOST, 2L, 100L, .22);
        double reserveBefore = cell.organicReserve();
        boolean changed = SoilSimulator.advance(cell, 120L, 2000L, new SoilSimulationContext(22, .65), RATES);
        assertTrue(changed);
        assertEquals(120L, cell.lastSimulatedDay());
        assertEquals(20L, cell.daysBare());
        assertTrue(cell.organicReserve() < reserveBefore);
        assertTrue(cell.profile().biology() > .35);
    }

    @Test void repeatedFamilyBuildsPressureAndRotationRelievesIt() {
        SoilCell cell = new SoilCell(0, 0, 64, new SoilProfile(.30, 6.4, .60, .20, .50, .35, .08), 1L, 100L);
        for (int i = 0; i < 3; i++) {
            cell.recordPlanting(CropFamily.GRAIN, 10 + i, 100 + i, 64);
            cell.recordCropRemoval(CropFamily.GRAIN, true, 20 + i, 100 + i);
        }
        double repeated = cell.continuousPressure();
        assertTrue(repeated >= .25);
        cell.recordPlanting(CropFamily.LEGUME, 40, 104, 64);
        assertTrue(cell.continuousPressure() < repeated);
    }

    @Test void noElapsedDaysIsNoOp() {
        SoilCell cell = new SoilCell(0, 0, 64, new SoilProfile(.30, 6.4, .60, .20, .50, .35, .08), 1L, 100L);
        assertFalse(SoilSimulator.advance(cell, 100L, 2L, new SoilSimulationContext(22, .65), RATES));
    }
}
