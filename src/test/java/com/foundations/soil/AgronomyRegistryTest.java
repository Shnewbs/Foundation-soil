package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentForm;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropFamily;
import com.foundations.soil.crop.CropRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgronomyRegistryTest {
    @AfterEach
    void reset() {
        AmendmentRegistry.resetToDefaults();
        CropRegistry.resetToDefaults();
    }

    @Test
    void externalAmendmentOverrideSurvivesDataReload() {
        AmendmentDefinition external = new AmendmentDefinition(
            "compost", AmendmentForm.DRY, .99, 0, 0, 0, 0, 0, 0, 0, 0, 1,
            "External Compost", "test override"
        );
        AmendmentRegistry.register(external);
        AmendmentRegistry.resetDataDefinitions();

        assertEquals("External Compost", AmendmentRegistry.resolve("compost").displayName());
        assertEquals(.99, AmendmentRegistry.resolve("compost").organicMatter(), 1.0e-9);
        assertTrue(AmendmentRegistry.externalCount() >= 1);
    }

    @Test
    void externalCropOverrideSurvivesDataReload() {
        ResourceLocation wheat = ResourceLocation.fromNamespaceAndPath("tfc", "crop/wheat");
        CropRegistry.register(wheat, new CropDefinition(CropFamily.GRAIN, 6.1, 6.2, .90, .10, .70, .80));
        CropRegistry.resetDataDefinitions();

        CropDefinition resolved = CropRegistry.resolve(wheat);
        assertEquals(6.1, resolved.preferredPHMin(), 1.0e-9);
        assertEquals(.90, resolved.compactionTolerance(), 1.0e-9);
        assertTrue(CropRegistry.externalCount() >= 1);
    }
}
