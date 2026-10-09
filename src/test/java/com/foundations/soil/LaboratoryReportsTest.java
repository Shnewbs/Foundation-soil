package com.foundations.soil;
import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.ledger.*;
import com.foundations.soil.soil.*;
import org.junit.jupiter.api.Test;
class LaboratoryReportsTest {
    @Test void reportsUseAllMembersNotOnlyThe64CellNetworkPage() {
        var ledger=new PlayerFieldLedger();var field=ledger.createField("Field trial");
        for(int i=0;i<150;i++) {
            var e=ledger.discover("minecraft:overworld",i,0,64,1);e.setFieldId(field.id());
            SoilCell c=new SoilCell(i,0,64,new SoilProfile(.4,6.5,.5,.1,.6,.6,.05),1,30);
            c.initializePhysical(SoilTexture.LOAM,"loam");c.enableMonitoring();e.refresh(c, 1, 30);
        }
        var report=SoilLaboratoryReports.describe(ledger,new LaboratorySelection(true,"",field.id()));
        assertTrue(report.stream().anyMatch(s->s.contains("150 discovered cells")));assertTrue(report.size()<96);
    }
    @Test void unknownOwnershipAndLegacyProbeDatesAreExplicit() {
        var ledger=new PlayerFieldLedger();var e=ledger.discover("minecraft:overworld",0,0,64,1);
        var report=SoilLaboratoryReports.describe(ledger,new LaboratorySelection(false,e.key(),0));
        assertTrue(report.stream().anyMatch(s->s.toLowerCase().contains("unknown")||s.toLowerCase().contains("not probed")));
        assertTrue(SoilLaboratoryReports.describe(new PlayerFieldLedger(),new LaboratorySelection(false,e.key(),0)).stream().anyMatch(s->s.contains("your")));
    }
}
