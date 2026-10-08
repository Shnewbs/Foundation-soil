package com.foundations.soil.network;

import com.foundations.soil.client.ClientSoilHudState;
import com.foundations.soil.client.ClientFieldGuideState;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class SoilNetwork {
    private static final double MAX_INSPECTION_DISTANCE_SQR = 12.0 * 12.0;

    private SoilNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("7");
        registrar.playToServer(SoilHudRequestPayload.TYPE, SoilHudRequestPayload.STREAM_CODEC, SoilNetwork::handleRequest);
        registrar.playToClient(SoilHudSnapshotPayload.TYPE, SoilHudSnapshotPayload.STREAM_CODEC,
            (payload, context) -> ClientSoilHudState.accept(payload));
        registrar.playToClient(FieldGuideDataPayload.TYPE, FieldGuideDataPayload.STREAM_CODEC,
            (payload, context) -> ClientFieldGuideState.accept(payload));
    }

    private static void handleRequest(SoilHudRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;

        BlockPos pos = BlockPos.of(payload.packedPos());
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_INSPECTION_DISTANCE_SQR) return;
        if (!player.serverLevel().hasChunkAt(pos)) return;

        BlockState state = player.serverLevel().getBlockState(pos);
        boolean probe = SoilHudTools.probeHeld(player);
        boolean hoe = SoilHudTools.hoeHeld(player);
        if (!probe && !hoe) return;
        if (probe) {
            if (!SoilTargeting.isSoil(state)) return;
        } else if (!SoilTargeting.isFarmland(state)) {
            return;
        }

        var cell = SoilManager.getOrCreate(player.serverLevel(), pos);
        context.reply(SoilHudSnapshotPayload.from(player.serverLevel(), pos, cell));
    }
}
