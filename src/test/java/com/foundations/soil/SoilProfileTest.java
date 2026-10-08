package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.soil.SoilProfile;
import org.junit.jupiter.api.Test;

class SoilProfileTest {
    @Test void clampsBounds() {
        SoilProfile p = new SoilProfile(2, 20, -1, 2, -1, 2, -1);
        assertEquals(1.0, p.organicMatter());
        assertEquals(9.5, p.pH());
        assertEquals(0.0, p.drainage());
        assertEquals(1.0, p.compaction());
    }

    @Test void limeRaisesPhAndCompostRaisesOrganicMatter() {
        SoilProfile p = new SoilProfile(.3, 5.5, .6, .2, .5, .4, .01);
        assertTrue(p.apply(Amendments.LIME).pH() > p.pH());
        assertTrue(p.apply(Amendments.COMPOST).organicMatter() > p.organicMatter());
    }

    @Test void nutrientModifierRespondsToCompaction() {
        SoilProfile loose = new SoilProfile(.4, 6.5, .6, .05, .7, .6, .01);
        SoilProfile compact = new SoilProfile(.4, 6.5, .6, .90, .7, .6, .01);
        assertTrue(loose.nutrientAvailabilityModifier() > compact.nutrientAvailabilityModifier());
    }
}
