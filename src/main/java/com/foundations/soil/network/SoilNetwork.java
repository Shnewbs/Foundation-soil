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

    private static final java.util.Map<ServerPlayer, RequestWindow> REQUESTS = new java.util.WeakHashMap<>();
    private static RequestWindow window(ServerPlayer player) { return REQUESTS.computeIfAbsent(player, ignored -> new RequestWindow()); }
    public static void clear() { REQUESTS.clear(); }
    public static boolean permitLedgerOpen(ServerPlayer player) { return window(player).read(player.level().getGameTime()); }
    private SoilNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("8");
        registrar.playToClient(FieldGuidePageReply.TYPE, FieldGuidePageReply.STREAM_CODEC,
            (payload, context) -> ClientFieldGuideState.acceptRefresh(payload.data()));
        registrar.playToClient(LaboratoryOpenPayload.TYPE, LaboratoryOpenPayload.STREAM_CODEC,
            (payload, context) -> com.foundations.soil.client.ClientLaboratoryState.accept(payload));
        registrar.playToClient(LaboratoryResultPayload.TYPE, LaboratoryResultPayload.STREAM_CODEC,
            (payload, context) -> com.foundations.soil.client.ClientLaboratoryState.accept(payload));
        registrar.playToServer(LaboratoryQueryPayload.TYPE, LaboratoryQueryPayload.STREAM_CODEC, SoilNetwork::laboratory);
        registrar.playToServer(LedgerEditPayload.TYPE, LedgerEditPayload.STREAM_CODEC, SoilNetwork::edit);
        registrar.playToServer(FieldGuidePageRequest.TYPE, FieldGuidePageRequest.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && window(player).read(player.level().getGameTime()))
                context.reply(new FieldGuidePageReply(FieldGuideDataPayload.from(player, payload.fieldOffset(), payload.cellOffset())));
        });
        registrar.playToClient(LedgerEditReply.TYPE, LedgerEditReply.STREAM_CODEC,
            (payload, context) -> com.foundations.soil.client.ClientLedgerEditState.accept(payload));
        registrar.playToServer(SoilHudRequestPayload.TYPE, SoilHudRequestPayload.STREAM_CODEC, SoilNetwork::handleRequest);
        registrar.playToClient(SoilHudSnapshotPayload.TYPE, SoilHudSnapshotPayload.STREAM_CODEC,
            (payload, context) -> ClientSoilHudState.accept(payload));
        registrar.playToClient(FieldGuideDataPayload.TYPE, FieldGuideDataPayload.STREAM_CODEC,
            (payload, context) -> ClientFieldGuideState.accept(payload));
    }

    private static void laboratory(LaboratoryQueryPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !window(player).read(player.level().getGameTime())) return;
        BlockPos position = BlockPos.of(payload.position());
        if (player.distanceToSqr(position.getX() + .5, position.getY() + .5, position.getZ() + .5) > 64
            || !player.level().hasChunkAt(position)
            || !player.level().getBlockState(position).is(com.foundations.soil.registry.SoilBlocks.LABORATORY.get())) {
            context.reply(new LaboratoryResultPayload(payload.requestId(), java.util.List.of("Return to the Soil Laboratory to request analysis."), java.util.List.of()));
            return;
        }
        context.reply(new LaboratoryResultPayload(payload.requestId(),
            com.foundations.soil.ledger.FieldLedgerManager.laboratoryReport(player, payload.first()),
            com.foundations.soil.ledger.FieldLedgerManager.laboratoryReport(player, payload.second())));
    }

    private static void edit(LedgerEditPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!window(player).edit(payload.requestId(), player.level().getGameTime())) {
            context.reply(new LedgerEditReply(payload.requestId(), "Duplicate or too-frequent request; reopen the guide to verify.", null));
            return;
        }
        var result = com.foundations.soil.ledger.FieldLedgerManager.mutate(player, payload.mutation());
        context.reply(new LedgerEditReply(payload.requestId(), result.message(), result.success()
            ? FieldGuideDataPayload.from(player, payload.fieldOffset(), payload.cellOffset()) : null));
    }

    private static void handleRequest(SoilHudRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !window(player).hud(player.level().getGameTime())) return;

        BlockPos pos = BlockPos.of(payload.packedPos());
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_INSPECTION_DISTANCE_SQR) return;
        if (!player.level().hasChunkAt(pos)) return;

        BlockState state = player.level().getBlockState(pos);
        boolean probe = SoilHudTools.probeHeld(player);
        boolean hoe = SoilHudTools.hoeHeld(player);
        if (!probe && !hoe) return;
        if (probe) {
            if (!SoilTargeting.isSoil(state)) return;
        } else if (!SoilTargeting.isFarmland(state)) {
            return;
        }

        var cell = SoilManager.getOrCreate(player.level(), pos);
        context.reply(SoilHudSnapshotPayload.from(player.level(), pos, cell));
    }
}
