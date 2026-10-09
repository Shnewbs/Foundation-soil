package com.foundations.soil.client;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.registry.SoilFluids;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

/** Native 26.1.2 material/tint models for both source and flowing amendment fluids. */
@EventBusSubscriber(modid = FoundationsSoil.MOD_ID, value = Dist.CLIENT)
public final class SoilFluidClientEvents {
    private SoilFluidClientEvents() {}
    @SubscribeEvent
    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        for (var family : SoilFluids.all()) {
            FluidModel.Unbaked model = new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/water_still"), true),
                new Material(Identifier.withDefaultNamespace("block/water_flow"), true), null,
                (FluidTintSource) state -> family.tint());
            event.register(model, family.source(), family.flowing());
        }
    }
}
