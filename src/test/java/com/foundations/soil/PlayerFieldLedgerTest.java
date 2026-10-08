package com.foundations.soil;

import com.foundations.soil.ledger.FieldLedgerField;
import com.foundations.soil.ledger.PlayerFieldLedger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PlayerFieldLedgerTest {
    @Test
    void fieldsGroupCellsWithoutDuplicatingMembershipLists() {
        PlayerFieldLedger ledger = new PlayerFieldLedger();
        var a = ledger.discover("minecraft:overworld", 1, 2, 64, 10L);
        var b = ledger.discover("minecraft:overworld", 2, 2, 64, 11L);
        FieldLedgerField field = ledger.createField("North Wheat Field");
        a.setFieldId(field.id());
        b.setFieldId(field.id());

        assertEquals(2, ledger.members(field.id()));
        assertEquals(2, ledger.entryCount());
        assertEquals(1, ledger.fieldCount());

        PlayerFieldLedger loaded = PlayerFieldLedger.load(ledger.save());
        assertEquals(2, loaded.members(field.id()));
        assertEquals("North Wheat Field", loaded.field(field.id()).orElseThrow().name());
    }

    @Test
    void deletingFieldLeavesCellsDiscoveredAndUnassigned() {
        PlayerFieldLedger ledger = new PlayerFieldLedger();
        var entry = ledger.discover("minecraft:overworld", 8, 8, 64, 10L);
        FieldLedgerField field = ledger.createField("Test");
        entry.setFieldId(field.id());

        assertTrue(ledger.deleteField(field.id()));
        assertEquals(1, ledger.entryCount());
        assertEquals(0, entry.fieldId());
        assertEquals(0, ledger.fieldCount());
    }
}
