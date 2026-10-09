package com.foundations.soil;
import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.soil.*;
import com.foundations.soil.api.*;
import com.foundations.soil.crop.*;
import org.junit.jupiter.api.Test;
class SoilPhysicalModelTest {
    private static final SoilProfile PROFILE = new SoilProfile(.45,6.5,.65,.10,.70,.70,.04);
    private static final CropDefinition CROP = new CropDefinition(CropFamily.GRAIN,6,7,.45,.3,.65,.4);
    @Test void compositionNormalizesAndRejectsInvalidInputs() {
        assertEquals(1,new SoilTexture(40,40,20).sand()+new SoilTexture(40,40,20).silt()+new SoilTexture(40,40,20).clay(),1e-12);
        assertEquals(.5,new SoilTexture(Double.MAX_VALUE,Double.MAX_VALUE,0).sand(),1e-12);
        assertThrows(IllegalArgumentException.class,()->new SoilTexture(0,0,0));
        assertThrows(IllegalArgumentException.class,()->new SoilTexture(Double.NaN,1,1));
        assertThrows(IllegalArgumentException.class,()->new SoilTexture(-1,1,1));
    }
    @Test void mineralsPersistAndCannotBeOverwrittenByAmendmentsOrMigration() {
        SoilCell c=new SoilCell(1,2,64,PROFILE,1,1);c.initializePhysical(SoilTexture.forSoil("tfc:dirt/clay_loam"),"tfc:dirt/clay_loam");
        c.initializePhysical(SoilTexture.forSoil("sand"),"wrong");
        SoilCell read=SoilCell.load(c.save());assertEquals(c.texture(),read.texture());assertEquals(PROFILE,read.profile());
        assertEquals("tfc:dirt/clay_loam",read.geologySource());
    }
    @Test void r6MigrationPreservesAllSevenStatisticsWithoutInventingGeology() {
        SoilCell c=new SoilCell(-7,5,64,PROFILE,9,23);var tag=c.save();tag.putInt("schema",3);tag.putInt("model",3);tag.remove("physical");
        SoilCell read=SoilCell.load(tag);assertEquals(PROFILE,read.profile());assertFalse(read.physicalInitialized());assertFalse(read.simulationBaselinePending());
        assertEquals(23,read.lastSimulatedDay());assertTrue(read.trend().points().isEmpty());
    }
    @Test void textureChangesRootZoneBehaviorWithoutChangingHydrationInput() {
        var wetProfile=new SoilProfile(.45,6.5,.55,.10,.70,.70,.04);
        var sand=SoilPhysics.evaluate(SoilTexture.forSoil("sand"),wetProfile,.95);
        var clay=SoilPhysics.evaluate(SoilTexture.forSoil("clay"),wetProfile,.95);
        assertTrue(sand.effectiveDrainage()>clay.effectiveDrainage());assertTrue(clay.waterHolding()>sand.waterHolding());
        assertEquals(SoilPhysics.Moisture.WATERLOGGED,clay.moisture());assertNotEquals(clay.moisture(),sand.moisture());
        assertThrows(IllegalArgumentException.class,()->SoilPhysics.evaluate(SoilTexture.LOAM,PROFILE,Double.NaN));
    }
    @Test void yieldScalingTouchesOnlyNewYieldAndCanBeDisabled() {
        assertEquals(.5f,SoilResponse.applyYield(.4f,.6f,.5,1),1e-6);
        assertEquals(.6f,SoilResponse.applyYield(.4f,.6f,.5,0),1e-6);
        assertEquals(.4f,SoilResponse.applyYield(.4f,.4f,.5,1),1e-6);
        assertEquals(.2f,SoilResponse.applyYield(.4f,.2f,.5,1),1e-6);
        assertThrows(IllegalArgumentException.class,()->SoilResponse.applyYield(0,1,Double.NaN,1));
    }
    @Test void badSoilProducesDifferentYieldAndTraitsCanImproveTolerance() {
        var stressed=new SoilProfile(.05,4.1,.15,.8,.15,.1,.85);
        double healthy=SoilResponse.evaluate(PROFILE,SoilTexture.LOAM,CROP,SoilTraits.NEUTRAL,.65,0).combined();
        double poor=SoilResponse.evaluate(stressed,SoilTexture.forSoil("clay"),CROP,SoilTraits.NEUTRAL,.95,.8).combined();
        assertTrue(healthy>poor);assertTrue(poor>=.2);
        var resistant=new SoilTraits(-1.5,.5,1,2,1);
        assertTrue(SoilResponse.evaluate(stressed,SoilTexture.LOAM,CROP,resistant,.95,.8).combined()>=poor);
        assertThrows(IllegalArgumentException.class,()->new SoilTraits(0,0,0,Double.POSITIVE_INFINITY,0));
    }
    @Test void nonFiniteSavedStateFailsInsteadOfSilentlyLosingAmendmentEffects() {
        var tag=new SoilCell(0,0,64,PROFILE,1,1).save();tag.putDouble("organic_reserve",Double.NaN);
        assertThrows(IllegalArgumentException.class,()->SoilCell.load(tag));
        assertThrows(IllegalArgumentException.class,()->new SoilSimulator.Rates(0,0,0,0,Double.NaN,0,0));
    }
}
