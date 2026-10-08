package com.foundations.soil;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentForm;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.soil.SoilProfile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class R8ValidationTest {
    @Test
    void rejectsNonFiniteAgronomyValues() {
        assertThrows(IllegalArgumentException.class,
            () -> new SoilProfile(Double.NaN, 6.5, .5, .1, .5, .5, .1));
        assertThrows(IllegalArgumentException.class,
            () -> new CropDefinition(CropFamily.GRAIN, 5.5, Double.POSITIVE_INFINITY, .5, .3, .6, .4));
        assertThrows(IllegalArgumentException.class,
            () -> new AmendmentDefinition("bad", AmendmentForm.DRY, 0, 0, 0, 0, 0, Double.NaN, 0, 0, .1, 1));
    }

    @Test
    void clampsValidButOutOfRangeSoilValues() {
        SoilProfile p = new SoilProfile(2, 15, -1, 2, .5, .5, .5);
        assertEquals(1.0, p.organicMatter());
        assertEquals(9.5, p.pH());
        assertEquals(0.0, p.drainage());
        assertEquals(1.0, p.compaction());
    }
}
