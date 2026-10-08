package com.foundations.soil.item;

import com.foundations.soil.ledger.FieldLedgerManager;
import com.foundations.soil.soil.SoilLabels;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilTargeting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class SoilProbeItem extends Item {
    public SoilProbeItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var target = SoilTargeting.find(context.getLevel(), context.getClickedPos());
        if (target.isEmpty()) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        var player = context.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        var cell = SoilManager.getOrCreate((ServerLevel) context.getLevel(), target.get());
        var ledgerEntry = FieldLedgerManager.scan(serverPlayer, (ServerLevel) context.getLevel(), target.get(), cell);
        var p = cell.profile();
        Component msg = Component.literal("Soil Cell [" + cell.cellX() + ", " + cell.cellZ() + "]")
            .withStyle(ChatFormatting.GOLD)
            .append(Component.literal(
                " | OM " + Math.round(p.organicMatter() * 100) + "% (" + SoilLabels.amount(p.organicMatter()) + ")"
                + " | pH " + String.format("%.2f", p.pH()) + " (" + SoilLabels.pH(p.pH()) + ")"
                + " | Drainage " + SoilLabels.quality(p.drainage())
                + " | Compaction " + Math.round(p.compaction() * 100) + "% (" + SoilLabels.compaction(p.compaction()) + ")"
                + " | Structure " + SoilLabels.quality(p.structure())
                + " | Biology " + Math.round(p.biology() * 100) + "%"
                + " | Salinity " + Math.round(p.salinity() * 100) + "%"
                + " | Nutrient access x" + String.format(java.util.Locale.ROOT, "%.2f", cell.nutrientAccessModifier())
                + " | Ledger " + (ledgerEntry.fieldId() == 0 ? "Unassigned" : "Field #" + ledgerEntry.fieldId())
            ).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(msg);
        return InteractionResult.SUCCESS;
    }
}
