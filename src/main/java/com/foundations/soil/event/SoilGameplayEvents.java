package com.foundations.soil.event;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.crop.CropDefinition;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import java.util.Optional;
import com.foundations.soil.integration.TfcRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = FoundationsSoil.MOD_ID)
public final class SoilGameplayEvents {
    private SoilGameplayEvents() {}

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (!SoilConfig.DAILY_UPDATES.get()) return;
        if (event.getServer().getTickCount() % 20 != 0) return;
        long day = TfcRuntime.days();
        int budget = SoilConfig.SIMULATION_BATCH_SIZE.get();
        for (ServerLevel level : event.getServer().getAllLevels()) SoilSimulationScheduler.tick(level, day, budget);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        SoilSimulationScheduler.clear();
        com.foundations.soil.network.SoilNetwork.clear();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!CropRegistry.isCultivatedCrop(event.getPlacedBlock())) return;
        findSoilBelow(level, event.getPos()).ifPresent(soil -> {
            CropDefinition def = CropRegistry.resolve(event.getPlacedBlock());
            SoilManager.recordPlanting(level, soil, def.family());
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!CropRegistry.isCultivatedCrop(event.getState())) return;
        findSoilBelow(level, event.getPos()).ifPresent(soil -> {
            CropDefinition def = CropRegistry.resolve(event.getState());
            boolean mature = TfcRuntime.isCrop(level.getBlockEntity(soil.above()))
                ? TfcRuntime.growth(level.getBlockEntity(soil.above())) >= 1.0f : CropRegistry.isMature(event.getState());
            SoilManager.recordCropRemoval(level, soil, def.family(), mature);
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void trampled(BlockEvent.FarmlandTrampleEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        SoilManager.recordDisturbance(level, event.getPos(), SoilConfig.TRAMPLE_COMPACTION.get(), SoilConfig.TRAMPLE_STRUCTURE.get());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tilled(BlockEvent.BlockToolModificationEvent event) {
        if (event.isCanceled() || event.isSimulated() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getItemAbility() != ItemAbilities.HOE_TILL) return;
        SoilTargeting.find(level, event.getPos()).ifPresent(pos -> SoilManager.recordDisturbance(level, pos, SoilConfig.TILL_COMPACTION.get(), SoilConfig.TILL_STRUCTURE.get()));
    }

    private static Optional<BlockPos> findSoilBelow(ServerLevel level, BlockPos cropPos) {
        BlockPos cursor = cropPos.below();
        for (int i = 0; i < 3; i++, cursor = cursor.below()) {
            if (SoilTargeting.isSoil(level.getBlockState(cursor))) return Optional.of(cursor.immutable());
        }
        return Optional.empty();
    }
}
