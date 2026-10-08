package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilProfile;
import org.junit.jupiter.api.Test;

class AmendmentEffectsTest {
    private static final SoilProfile BASE = new SoilProfile(.35, 6.2, .60, .20, .55, .45, .02);

    @Test void builtInAmendmentsRemainDistinct() {
        assertTrue(BASE.apply(Amendments.LIME).pH() > BASE.pH());
        assertTrue(BASE.apply(Amendments.SULFUR).pH() < BASE.pH());
        assertTrue(BASE.apply(Amendments.CLAY).structure() > BASE.structure());
        assertTrue(BASE.apply(Amendments.COMPOST).biology() > BASE.biology());
        assertTrue(BASE.apply(Amendments.BIOCHAR).drainage() > BASE.drainage());
        assertTrue(BASE.apply(Amendments.MANURE).organicMatter() > BASE.organicMatter());
    }

    @Test void repeatedApplicationHasDiminishingResponseAndKeepsReserve() {
        SoilCell cell = new SoilCell(0, 0, 64, BASE, 1L, 1L);
        double first = cell.apply(Amendments.COMPOST, 2L, 1L, .22);
        double second = cell.apply(Amendments.COMPOST, 3L, 1L, .22);
        assertEquals(1.0, first, 0.000001);
        assertTrue(second < first);
        assertTrue(cell.organicReserve() > 0);
    }

    @Test void builtInsDeclareFutureDeliveryCompatibility() {
        assertEquals(com.foundations.soil.amendment.AmendmentForm.SUSPENDABLE, Amendments.LIME.form());
        assertEquals(com.foundations.soil.amendment.AmendmentForm.SUSPENDABLE, Amendments.COMPOST.form());
    }

    @Test void repeatedApplicationNeverEscapesModelBounds() {
        SoilProfile p = BASE;
        for (int i = 0; i < 1000; i++) p = p.apply(Amendments.COMPOST);
        assertTrue(p.organicMatter() <= 1.0);
        assertTrue(p.biology() <= 1.0);
        assertTrue(p.structure() <= 1.0);
        assertTrue(p.salinity() <= 1.0);
    }
}
