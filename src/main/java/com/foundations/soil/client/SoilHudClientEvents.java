package com.foundations.soil.client;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.config.SoilClientConfig;
import com.foundations.soil.network.SoilHudRequestPayload;
import com.foundations.soil.network.SoilHudTools;
import com.foundations.soil.soil.SoilTargeting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = FoundationsSoil.MOD_ID, value = Dist.CLIENT)
public final class SoilHudClientEvents {
    private static int requestCooldown;

    private SoilHudClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var guidePayload = ClientFieldGuideState.take();
        if (guidePayload != null) {
            mc.setScreen(new FieldGuideScreen(guidePayload));
        }
        if (mc.player == null || mc.level == null) {
            ClientSoilHudState.clear();
            return;
        }
        if (!SoilClientConfig.HUD_ENABLED.get()) {
            ClientSoilHudState.clear();
            return;
        }

        boolean probe = SoilHudTools.probeHeld(mc.player);
        boolean hoe = SoilClientConfig.HUD_SHOW_WITH_HOE.get() && SoilHudTools.hoeHeld(mc.player);
        if (!probe && !hoe) {
            ClientSoilHudState.clear();
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            ClientSoilHudState.clear();
            return;
        }

        var target = SoilTargeting.find(mc.level, hit.getBlockPos());
        if (target.isEmpty()) {
            ClientSoilHudState.clear();
            return;
        }

        BlockPos pos = target.get();
        if (!probe && !SoilTargeting.isFarmland(mc.level.getBlockState(pos))) {
            ClientSoilHudState.clear();
            return;
        }

        long packed = pos.asLong();
        boolean changed = ClientSoilHudState.requestedPos() != packed;
        ClientSoilHudState.target(packed);

        if (changed || requestCooldown-- <= 0) {
            PacketDistributor.sendToServer(new SoilHudRequestPayload(packed));
            requestCooldown = 4;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSoilHudState.clear();
        ClientFieldGuideState.clear();
        requestCooldown = 0;
    }
}
