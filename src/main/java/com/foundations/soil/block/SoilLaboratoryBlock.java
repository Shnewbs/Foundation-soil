package com.foundations.soil.block;

import com.foundations.soil.network.FieldGuideDataPayload;
import com.foundations.soil.network.LaboratoryOpenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/** Analysis is request-driven; this block deliberately has no ticking block entity. */
public final class SoilLaboratoryBlock extends Block {
    private static final com.mojang.serialization.MapCodec<SoilLaboratoryBlock> CODEC = simpleCodec(SoilLaboratoryBlock::new);
    @Override public com.mojang.serialization.MapCodec<SoilLaboratoryBlock> codec() { return CODEC; }
    public SoilLaboratoryBlock(Properties properties) { super(properties); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && com.foundations.soil.network.SoilNetwork.permitLedgerOpen(serverPlayer))
            PacketDistributor.sendToPlayer(serverPlayer, new LaboratoryOpenPayload(pos.asLong(), FieldGuideDataPayload.from(serverPlayer)));
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
