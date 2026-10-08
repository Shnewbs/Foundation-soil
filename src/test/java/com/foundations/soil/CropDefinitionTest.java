package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.soil.SoilProfile;
import org.junit.jupiter.api.Test;

class CropDefinitionTest {
    @Test void compactAcidicSoilScoresBelowPreferredSoil() {
        CropDefinition crop = new CropDefinition(CropFamily.GRAIN, 5.8, 7.2, .5, .35, .65, .5);
        SoilProfile good = new SoilProfile(.45, 6.4, .65, .15, .7, .6, .01);
        SoilProfile poor = new SoilProfile(.20, 4.5, .25, .85, .4, .2, .10);
        assertTrue(crop.soilResponse(good) > crop.soilResponse(poor));
    }
}
