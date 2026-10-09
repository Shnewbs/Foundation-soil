package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.integration.TfcRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

/** Only the isolated dependency-free model suite runs this negative test. */
@Tag("model-only")
class TfcMissingDependencyTest {
    @Test void missingTfcFailsClearlyAndDoesNotInventCalendarOrSoilValues() {
        var first=assertThrows(IllegalStateException.class,TfcRuntime::bootstrap);
        assertTrue(first.getMessage().contains("no vanilla"));
        assertThrows(IllegalStateException.class,TfcRuntime::ticks);
        assertThrows(IllegalStateException.class,TfcRuntime::days);
    }
}
