package com.foundations.soil.item;

import com.foundations.soil.network.FieldGuideDataPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Player-facing book that renders the server-authoritative Field Ledger as a client snapshot. */
public final class FieldGuideItem extends Item {
    public FieldGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && com.foundations.soil.network.SoilNetwork.permitLedgerOpen(serverPlayer)) {
            PacketDistributor.sendToPlayer(serverPlayer, FieldGuideDataPayload.from(serverPlayer));
        }
        return InteractionResult.SUCCESS;
    }
}
