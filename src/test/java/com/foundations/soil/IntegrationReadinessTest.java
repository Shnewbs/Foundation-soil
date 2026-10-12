package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.integration.FrameworkSoilBridge;
import com.foundations.soil.integration.TfcRuntime;
import com.foundations.soil.ledger.FieldLedgerData;
import com.foundations.soil.soil.SoilCell;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("model-only")
class IntegrationReadinessTest {
    @Test void persistentVersionsMatchNativeBetaContract() {
        assertEquals(4, SoilCell.SCHEMA_VERSION);
        assertEquals(4, SoilCell.MODEL_VERSION);
        assertEquals(5, FieldLedgerData.SCHEMA_VERSION);
    }

    @Test void missingTfcCannotPretendToBeReady() {
        var failure = assertThrows(IllegalStateException.class, TfcRuntime::diagnostic);
        assertTrue(failure.getMessage().contains("compatible native 26.1.2"));
    }

    @Test void missingRequiredFrameworkCannotContinueStartup() {
        var failure = assertThrows(IllegalStateException.class, FrameworkSoilBridge::bootstrapRequired);
        assertTrue(failure.getMessage().contains("required"));
        assertNotEquals(FrameworkSoilBridge.Status.ACTIVE, FrameworkSoilBridge.status());
    }
}
