package com.foundations.soil;

import com.foundations.soil.integration.SoilIntegrationRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoilIntegrationRegistryTest {
    @Test
    void tracksCapabilitiesAndOperationCounters() {
        Identifier id = Identifier.fromNamespaceAndPath("foundations_soil_test", "genetics");
        SoilIntegrationRegistry.register(id, "Genetics Test", java.util.Set.of("crop_registration", "soil_query"));
        SoilIntegrationRegistry.record(id, "crops_registered");
        SoilIntegrationRegistry.record(id, "crops_registered");
        long duplicatesBefore = SoilIntegrationRegistry.duplicateRegistrationsSuppressed();
        SoilIntegrationRegistry.register(id, "Genetics Test", java.util.Set.of("crop_registration", "soil_query"));

        var snapshot = SoilIntegrationRegistry.snapshot().get(id);
        assertNotNull(snapshot);
        assertEquals("Genetics Test", snapshot.displayName());
        assertTrue(snapshot.capabilities().contains("soil_query"));
        assertEquals(2, snapshot.counters().get("crops_registered"));
        assertEquals(duplicatesBefore + 1, SoilIntegrationRegistry.duplicateRegistrationsSuppressed());
    }
}
