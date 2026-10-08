package com.foundations.soil.definition;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.integration.FrameworkSoilBridge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@EventBusSubscriber(modid = FoundationsSoil.MOD_ID)
public final class AgronomyDefinitionEvents {
    private AgronomyDefinitionEvents() {}

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        FrameworkSoilBridge.bootstrap();
        var report = AgronomyDefinitionLoader.reload();
        FoundationsSoil.LOGGER.info("[Foundations Soil] Server config active — preset={} simulationScale={}x amendmentScale={}x bagUses={} manureCooldown={}d",
            SoilConfig.preset(), String.format(java.util.Locale.ROOT, "%.2f", SoilConfig.effectiveSimulationMultiplier()),
            String.format(java.util.Locale.ROOT, "%.2f", SoilConfig.effectiveAmendmentStrength()),
            SoilConfig.APPLICATIONS_PER_BAG.get(), SoilConfig.MANURE_COLLECTION_COOLDOWN_DAYS.get());
        FoundationsSoil.LOGGER.info("[Foundations Soil] Agronomy runtime ready — amendments={} cropFamilies={} cropOverrides={} soilProfiles={} rejected={} replaced={} preservedExternal={}",
            report.amendments(), report.cropFamilies(), report.cropOverrides(), report.soilProfiles(), report.errors(), report.replaced(), report.preservedExternal());
    }
}
