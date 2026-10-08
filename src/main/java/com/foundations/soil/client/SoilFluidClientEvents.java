package com.foundations.soil.client;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.registry.SoilFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-only water-texture rendering for the five physical amendment fluids. */
@EventBusSubscriber(modid = FoundationsSoil.MOD_ID, value = Dist.CLIENT)
public final class SoilFluidClientEvents {
    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    private SoilFluidClientEvents() {}


    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (SoilFluids.FluidFamily family : SoilFluids.all()) {
                ItemBlockRenderTypes.setRenderLayer(family.source().get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(family.flowing().get(), RenderType.translucent());
            }
        });
    }

    @SubscribeEvent
    public static void registerFluidExtensions(RegisterClientExtensionsEvent event) {
        for (SoilFluids.FluidFamily family : SoilFluids.all()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override public ResourceLocation getStillTexture() { return WATER_STILL; }
                @Override public ResourceLocation getFlowingTexture() { return WATER_FLOW; }
                @Override public int getTintColor() { return family.tint(); }
            }, family.type().get());
        }
    }
}
