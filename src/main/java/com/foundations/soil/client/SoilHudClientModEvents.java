package com.foundations.soil.client;

import com.foundations.soil.FoundationsSoil;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = FoundationsSoil.MOD_ID, value = Dist.CLIENT)
public final class SoilHudClientModEvents {
    private static final Identifier SOIL_HUD = Identifier.fromNamespaceAndPath(
        FoundationsSoil.MOD_ID, "soil_hud"
    );

    private SoilHudClientModEvents() {}

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(SOIL_HUD, SoilHudOverlay::render);
    }
}
