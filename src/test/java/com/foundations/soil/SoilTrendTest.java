package com.foundations.soil;
import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.soil.*;
import org.junit.jupiter.api.Test;
class SoilTrendTest {
    private static final SoilProfile P=new SoilProfile(.4,6.5,.5,.1,.6,.6,.05);
    @Test void historyIsBoundedAndSameDayObservationsReplaceRatherThanMultiply() {
        SoilTrend t=new SoilTrend();for(int day=0;day<100;day++)t.record(day,P,.1);
        assertEquals(16,t.points().size());assertEquals(84,t.points().getFirst().day());
        t.record(99,P,.3);assertEquals(16,t.points().size());assertEquals(.3,t.points().getLast().cropPressure());
        t.record(98,P,.9);assertEquals(.3,t.points().getLast().cropPressure());
    }
    @Test void snapshotsAreImmutableAndSaveRoundTrips() {
        SoilTrend t=new SoilTrend();t.record(4,P,.2);var points=t.points();
        assertThrows(UnsupportedOperationException.class,()->points.clear());t.record(5,P,.1);assertEquals(1,points.size());
        SoilTrend read=new SoilTrend();read.load(t.save());assertEquals(t.points(),read.points());
    }
    @Test void monitoringDoesNotInventDaysBeforeTheFirstProbe() {
        SoilCell c=new SoilCell(0,0,64,P,5,40);assertTrue(c.trend().points().isEmpty());c.enableMonitoring();
        assertEquals(1,c.trend().points().size());assertEquals(40,c.trend().points().getFirst().day());
        assertEquals(c.trend().points(),SoilCell.load(c.save()).trend().points());
    }
    @Test void plantingAndRemovalReplayStateSurvivesRestart() {
        SoilCell c=new SoilCell(-1,-1,64,P,1,1);
        assertTrue(c.markPlanting(-1,-1,true));assertFalse(c.markPlanting(-1,-1,true));
        c=SoilCell.load(c.save());assertFalse(c.markPlanting(-1,-1,true));assertTrue(c.markPlanting(-1,-1,false));
        assertFalse(c.markPlanting(-1,-1,false));assertTrue(c.markPlanting(-1,-1,true));
    }
    @Test void repeatedPickingKeepsPlantAliveAndDoesNotInventResidue() {
        var cell=new com.foundations.soil.soil.SoilCell(0,0,new com.foundations.soil.soil.SoilProfile(.4,6.5,.6,.1,.7,.5,.02),0);
        var family=com.foundations.soil.crop.CropFamily.OTHER;
        cell.reconcilePlantings(1,family.id()); double residue=cell.residueReserve();
        cell.recordPickingHarvest(family,100,2);cell.recordPickingHarvest(family,200,3);
        assertEquals(1,cell.activePlantings()); assertEquals(2,cell.cropCycles());
        assertEquals(residue,cell.residueReserve()); assertEquals(family.id(),cell.currentCropFamily());
        var restored=com.foundations.soil.soil.SoilCell.load(cell.save());
        assertEquals(1,restored.activePlantings()); assertEquals(2,restored.cropCycles());
    }
}
