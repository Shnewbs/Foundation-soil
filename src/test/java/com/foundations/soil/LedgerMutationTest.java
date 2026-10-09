package com.foundations.soil;
import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.ledger.*;
import org.junit.jupiter.api.Test;
class LedgerMutationTest {
    private static LedgerMutation m(LedgerMutation.Action a,String cell,int field,String name,String note,boolean favorite){return new LedgerMutation(a,cell,field,name,note,favorite);}
    @Test void createAssignEditAndDeletePreserveDiscoveredCells() {
        var ledger=new PlayerFieldLedger();var cell=ledger.discover("minecraft:overworld",1,2,64,1);
        assertTrue(m(LedgerMutation.Action.CREATE_FIELD,cell.key(),0,"North","cover crop trial",true).apply(ledger).success());
        int id=cell.fieldId();assertTrue(id>0);assertEquals(1,ledger.members(id));
        assertTrue(m(LedgerMutation.Action.SAVE_CELL,cell.key(),id,"","My notes",true).apply(ledger).success());
        assertTrue(cell.favorite());assertEquals("My notes",cell.note());
        assertTrue(m(LedgerMutation.Action.SAVE_FIELD,"",id,"North plot","new note",false).apply(ledger).success());
        var restored=PlayerFieldLedger.load(ledger.save());assertEquals("North plot",restored.field(id).orElseThrow().name());
        assertTrue(m(LedgerMutation.Action.DELETE_FIELD,"",id,"","",false).apply(ledger).success());
        assertEquals(0,cell.fieldId());assertEquals(1,ledger.entryCount());assertEquals("My notes",cell.note());
    }
    @Test void callerCannotAssignUndiscoveredCellsOrAnotherOwnersField() {
        var own=new PlayerFieldLedger();var other=new PlayerFieldLedger();var foreignCell=other.discover("minecraft:overworld",3,4,64,1);other.createField("Private");
        assertFalse(m(LedgerMutation.Action.SAVE_CELL,foreignCell.key(),1,"","stolen",true).apply(own).success());
        assertEquals(0,own.entryCount());assertEquals("",foreignCell.note());
        var cell=own.discover("minecraft:overworld",7,8,64,1);
        assertFalse(m(LedgerMutation.Action.SAVE_CELL,cell.key(),1,"","must not change",true).apply(own).success());
        assertEquals("",cell.note());assertFalse(cell.favorite());
    }
    @Test void validationIsAtomicAndDuplicateNamesDoNotAllocateNewFields() {
        var ledger=new PlayerFieldLedger();
        assertFalse(m(LedgerMutation.Action.CREATE_FIELD,"not-discovered",0,"North","",false).apply(ledger).success());assertEquals(0,ledger.fieldCount());
        assertTrue(m(LedgerMutation.Action.CREATE_FIELD,"",0,"North","",false).apply(ledger).success());
        assertFalse(m(LedgerMutation.Action.CREATE_FIELD,"",0,"NORTH","",false).apply(ledger).success());assertEquals(1,ledger.fieldCount());
        assertFalse(m(LedgerMutation.Action.SAVE_FIELD,"",1,"  ","",false).apply(ledger).success());assertEquals("North",ledger.field(1).orElseThrow().name());
    }
    @Test void textLimitsRejectOversizeAndRemoveControlCharactersWithoutSplittingEmoji() {
        assertThrows(IllegalArgumentException.class,()->m(LedgerMutation.Action.CREATE_FIELD,"",0,"a".repeat(65),"",false));
        assertEquals("a😀",LedgerText.clean("a😀b",3));assertEquals("ab",LedgerText.clean("a\u202Eb\u0000",20));
        assertEquals("a",LedgerText.clean("a😀",2));
    }
}
